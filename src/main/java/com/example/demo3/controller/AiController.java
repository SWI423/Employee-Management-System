package com.example.demo3.controller;

import com.example.demo3.common.Result;
import com.example.demo3.dto.AiChatDTO;
import com.example.demo3.service.AiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 智能助手控制器（独立模块，不动原有员工/部门接口）
 * 场景示例：
 *   "帮我查技术部所有女员工"   → 大模型决策 → query_emp(department=技术部, gender=女)
 *   "统计各部门人数"          → 大模型决策 → count_emp_by_dept()
 * 需登录（携带 Authorization: Bearer {token}），复用原有拦截器
 */
@Tag(name = "AI 智能助手", description = "自然语言查员工/部门（智谱 GLM-4-Flash + function calling，LLM决策→工具调用→结果回填→人话回复）")
@RestController
@RequestMapping("/ai")
public class AiController {

    @Resource
    private AiService aiService;

    @Operation(summary = "AI 对话", description = "输入自然语言问题，如：帮我查技术部所有女员工 / 统计各部门人数；返回 AI 组织好的人话回复")
    @PostMapping("/chat")
    public Result<String> chat(@Valid @RequestBody AiChatDTO dto) {
        return Result.success(aiService.chat(dto.getMessage()));
    }
}
