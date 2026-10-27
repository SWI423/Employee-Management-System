package com.example.demo3.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * AI 对话请求体：用户输入一句自然语言
 * 示例："帮我查技术部所有女员工" / "统计各部门人数"
 */
@Data
public class AiChatDTO {

    @NotBlank(message = "问题不能为空")
    private String message;
}
