import request from './request'

/**
 * AI 智能助手：自然语言查员工/统计部门人数
 * 后端 POST /ai/chat：LLM 决策 → 工具调用（查真实库）→ 结果回填 → LLM 组织回复
 * 单独设置 60s 超时：AI 链路含多轮 LLM 调用，比普通接口慢
 */
export function aiChat(message) {
  return request.post('/ai/chat', { message }, { timeout: 60000 })
}
