package com.example.demo3.service;

/**
 * AI 智能助手服务：自然语言查员工/部门数据
 * 流程：用户问题 → 大模型决策（是否调用工具）→ 执行查询工具 → 结果回填 → 大模型组织人话回复
 */
public interface AiService {

    /**
     * 与 AI 助手对话
     * @param userMessage 用户自然语言问题，如"帮我查技术部所有女员工"
     * @return AI 组织好的人类可读回复
     */
    String chat(String userMessage);
}
