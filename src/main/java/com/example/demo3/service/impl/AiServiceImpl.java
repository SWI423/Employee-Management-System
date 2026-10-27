package com.example.demo3.service.impl;

import com.example.demo3.service.AiService;
import com.example.demo3.service.AiToolExecutor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * AI 智能助手业务实现（智谱 GLM-4-Flash，OpenAI 兼容协议，支持 function calling）
 *
 * 双通道设计（面试演示核心）：
 *   主通道（真 function calling）：用户问题 → 大模型决策 → 返回 tool_calls
 *       → 执行工具（AiToolExecutor 查真实数据库）→ 结果 role=tool 回填 → 大模型组织人话回复
 *   兜底通道（规则路由 + LLM 参数抽取）：免费模型 GLM-4-Flash 偶发"不触发工具调用"，
 *       第一轮未触发时用关键词规则确定该调哪个工具；名单查询的参数（部门/岗位/性别）
 *       仍交给大模型抽取；执行结果同样回填给大模型组织回复。
 *   两条通道共用同一套工具执行与回填逻辑，保证任何问法都能稳定跑通完整链路。
 *
 * 最多 3 轮，防止死循环。
 */
@Slf4j
@Service
public class AiServiceImpl implements AiService {

    /** 最多对话轮数：工具调用-回填-再回复算 2 轮，3 轮足够覆盖"先查再统计再答复"的复杂场景 */
    private static final int MAX_ROUND = 3;

    /** 系统提示词：实测精简版对 GLM-4-Flash 工具触发最稳（详细罗列工具反而干扰模型决策） */
    private static final String SYSTEM_PROMPT = """
            你是"企业员工信息管理系统"的智能助手。规则：涉及员工或部门数据时，必须先调用工具获取真实数据，禁止编造。
            统计部门人数→调用 count_emp_by_dept；查询员工名单→调用 query_emp。
            回答用简洁中文，查不到如实说明。
            """;

    /** 第一轮未触发工具调用时的强提示重问（GLM-4-Flash 偶发不触发工具，重问一次可显著提升稳定性） */
    private static final String REASK_PROMPT = """
            请重新判断用户意图：涉及员工名单/条件查询，请调用 query_emp 工具；涉及部门人数统计，请调用 count_emp_by_dept 工具。
            必须先调用工具获取真实数据，再组织回答，禁止直接回答或编造数据。
            """;

    /** 参数抽取提示词：让大模型把自然语言问题转成结构化查询参数（兜底通道用，不带 tools） */
    private static final String EXTRACT_PROMPT = """
            你是参数抽取器。从用户问题中抽取员工查询参数，只输出一个 JSON 对象，字段可选：
            department（部门名称）、job（岗位名称）、gender（性别，男/女）。
            问题中没有的字段不要出现，不要输出 JSON 以外的任何文字。
            """;

    /** tools 声明：OpenAI 兼容的 function calling 格式（count_emp_by_dept 前置、参数精简，
     *  实测 GLM-4-Flash 对复杂函数定义响应不稳定，越精简越稳） */
    private static final String TOOLS_JSON = """
            [
              {
                "type": "function",
                "function": {
                  "name": "count_emp_by_dept",
                  "description": "统计各部门人数，当用户问各部门人数/每个部门多少人时调用。",
                  "parameters": {"type": "object", "properties": {}, "required": []}
                }
              },
              {
                "type": "function",
                "function": {
                  "name": "query_emp",
                  "description": "查询员工名单，当用户问查员工/某部门的员工/某性别的员工时调用。",
                  "parameters": {
                    "type": "object",
                    "properties": {
                      "department": {"type": "string"},
                      "gender": {"type": "string"},
                      "job": {"type": "string"}
                    },
                    "required": []
                  }
                }
              }
            ]
            """;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final AiToolExecutor toolExecutor;

    @Value("${ai.glm.api-key}")
    private String apiKey;
    @Value("${ai.glm.base-url}")
    private String baseUrl;
    @Value("${ai.glm.model}")
    private String model;

    public AiServiceImpl(RestTemplate restTemplate, ObjectMapper objectMapper, AiToolExecutor toolExecutor) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.toolExecutor = toolExecutor;
    }

    @Override
    public String chat(String userMessage) {
        // 本地未配置 AI key（application-local.yml 或环境变量 AI_GLM_API_KEY）时给出友好提示，不抛 500
        if (apiKey == null || apiKey.isBlank()) {
            return "AI 服务未配置 API Key：请在本地创建 src/main/resources/application-local.yml（模板见 README）或设置环境变量 AI_GLM_API_KEY。";
        }
        // 1. 组装初始消息：系统提示（含意图先验引导） + 用户问题
        ArrayNode messages = objectMapper.createArrayNode();
        addTextMessage(messages, "system", SYSTEM_PROMPT + intentHint(userMessage));
        addTextMessage(messages, "user", userMessage);

        // 2. 多轮循环：决策 → 工具调用 → 结果回填 → 直到大模型给出最终回答
        for (int round = 1; round <= MAX_ROUND; round++) {
            JsonNode resp = callLlm(messages, true);
            JsonNode message = resp.path("choices").path(0).path("message");
            String content = message.path("content").asText(null);
            JsonNode toolCalls = message.path("tool_calls");

            // 情况一：大模型主动触发工具 → 真 function calling 主链路
            if (!toolCalls.isMissingNode() && !toolCalls.isEmpty()) {
                messages.add(message); // 原样保留 assistant 的 tool_calls 消息
                for (JsonNode call : toolCalls) {
                    String fnName = call.path("function").path("name").asText();
                    String fnArgs = call.path("function").path("arguments").asText();
                    log.info("[AI] 第{}轮 大模型决策调用: {} 参数: {}", round, fnName, fnArgs);
                    feedToolResult(messages, call.path("id").asText(), fnName, fnArgs, round);
                }
                continue; // 下一轮让大模型看到工具结果后组织回复
            }

            // 情况二：大模型未触发工具
            if (round == 1) {
                // 2a. 规则路由兜底：按关键词确定工具并执行（保证演示稳定）
                String[] route = routeByRule(userMessage);
                if (route != null) {
                    log.warn("[AI] 第一轮未触发工具调用，规则路由兜底执行: {} {}", route[0], route[1]);
                    feedToolResult(messages, "call_route_1", route[0], route[1], round);
                    continue;
                }
                // 2b. 意图不明 → 强提示重问一轮
                log.warn("[AI] 第一轮未触发工具调用且意图不明，强提示重问：{}",
                        content == null ? "（无内容）" : content.trim());
                addTextMessage(messages, "user", REASK_PROMPT);
                continue;
            }

            // 3 轮后仍未走工具 → 返回大模型的内容（如寒暄类问题）
            if (content != null && !content.isBlank()) {
                return content.trim();
            }
            return "抱歉，我没有理解你的问题，请换个说法试试。";
        }
        return "处理轮次超限，请稍后重试。";
    }

    /**
     * 执行工具并回填结果（主链路与兜底链路共用）：
     *   按 OpenAI 规范补一条 assistant tool_calls 消息 + role=tool 结果消息
     */
    private void feedToolResult(ArrayNode messages, String toolCallId, String fnName, String fnArgs, int round) {
        // 补 assistant 的 tool_calls 声明（规范要求 tool 消息之前有对应的 assistant 消息）
        ObjectNode asst = objectMapper.createObjectNode();
        asst.put("role", "assistant");
        asst.put("content", (String) null);
        ArrayNode calls = objectMapper.createArrayNode();
        ObjectNode call = objectMapper.createObjectNode();
        call.put("id", toolCallId);
        call.put("type", "function");
        ObjectNode fn = objectMapper.createObjectNode();
        fn.put("name", fnName);
        fn.put("arguments", fnArgs);
        call.set("function", fn);
        calls.add(call);
        asst.set("tool_calls", calls);
        messages.add(asst);

        // 执行真实数据库查询
        String toolResult = toolExecutor.execute(fnName, fnArgs);
        log.info("[AI] 第{}轮 工具结果: {}", round, toolResult);

        // role=tool 回填
        ObjectNode toolMsg = objectMapper.createObjectNode();
        toolMsg.put("role", "tool");
        toolMsg.put("tool_call_id", toolCallId);
        toolMsg.put("content", toolResult);
        messages.add(toolMsg);
    }

    /**
     * 规则路由：关键词预判该走哪个工具（兜底通道的第一步，保证演示稳定）
     * 返回 [工具名, 参数JSON]；意图不明返回 null
     */
    private String[] routeByRule(String userMessage) {
        if (containsAny(userMessage, "统计", "人数", "每个部门", "各部门", "多少人", "几名", "分布")) {
            return new String[]{"count_emp_by_dept", "{}"};
        }
        if (containsAny(userMessage, "查", "找", "有哪些", "名单", "员工")) {
            // 名单查询：查询条件由大模型抽取（部门/岗位/性别）
            String args = extractQueryArgs(userMessage);
            return new String[]{"query_emp", args};
        }
        return null;
    }

    /**
     * 参数抽取：无 tools 的一次 LLM 调用，把自然语言转成查询参数 JSON
     * 失败降级为空对象，保证链路不中断
     */
    private String extractQueryArgs(String userMessage) {
        try {
            ArrayNode messages = objectMapper.createArrayNode();
            addTextMessage(messages, "system", EXTRACT_PROMPT);
            addTextMessage(messages, "user", userMessage);
            JsonNode resp = callLlm(messages, false);
            String content = resp.path("choices").path(0).path("message").path("content").asText("{}");
            // 清洗：去 markdown 代码块标记，截取第一个 { 到最后一个 }
            String cleaned = content.trim()
                    .replaceAll("```json", "").replaceAll("```", "").trim();
            int start = cleaned.indexOf('{');
            int end = cleaned.lastIndexOf('}');
            if (start >= 0 && end > start) {
                cleaned = cleaned.substring(start, end + 1);
            }
            JsonNode args = objectMapper.readTree(cleaned);
            log.info("[AI] 参数抽取结果: {}", args);
            return args.toString();
        } catch (Exception e) {
            log.warn("[AI] 参数抽取失败，使用空参数查询: {}", e.getMessage());
            return "{}";
        }
    }

    /**
     * 调用智谱大模型：POST {base}/chat/completions，OpenAI 兼容
     * @param withTools 是否携带 tools 声明（参数抽取场景不携带）
     */
    private JsonNode callLlm(ArrayNode messages, boolean withTools) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", model);
        body.set("messages", messages);
        if (withTools) {
            try {
                body.set("tools", objectMapper.readTree(TOOLS_JSON));
            } catch (Exception e) {
                throw new IllegalStateException("工具定义解析失败", e);
            }
        }
        body.put("temperature", 0.2);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        HttpEntity<String> entity = new HttpEntity<>(body.toString(), headers);

        String url = baseUrl + "/chat/completions";
        String respBody = restTemplate.postForObject(url, entity, String.class);
        try {
            return objectMapper.readTree(respBody);
        } catch (Exception e) {
            throw new IllegalStateException("AI 响应解析失败: " + respBody, e);
        }
    }

    private void addTextMessage(ArrayNode messages, String role, String content) {
        ObjectNode msg = objectMapper.createObjectNode();
        msg.put("role", role);
        msg.put("content", content);
        messages.add(msg);
    }

    /**
     * 意图先验引导：轻量关键词规则预判问题类型，把"该调哪个工具"作为提示注入系统消息。
     * 说明：这只是降低免费模型偶发"不触发工具 / 选错工具"的概率，最终仍优先走大模型决策。
     */
    private String intentHint(String userMessage) {
        if (containsAny(userMessage, "统计", "人数", "每个部门", "各部门", "多少人", "几名", "分布")) {
            return "\n【意图提示】本次提问属于“部门人数统计”类，请调用 count_emp_by_dept 工具获取各部门人数后再回答。";
        }
        if (containsAny(userMessage, "查", "找", "有哪些", "名单", "员工")) {
            return "\n【意图提示】本次提问属于“员工名单查询”类，请调用 query_emp 工具，并按问题内容填写 department/job/gender 参数后再回答。";
        }
        return "";
    }

    private boolean containsAny(String text, String... keywords) {
        for (String kw : keywords) {
            if (text != null && text.contains(kw)) {
                return true;
            }
        }
        return false;
    }
}
