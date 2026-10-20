package com.example.demo3.common;

import lombok.Getter;

/**
 * 错误码枚举：统一管理全系统错误码，避免魔法数字散落各处
 */
@Getter
public enum ErrorCode {

    // ---------- 参数类 ----------
    PARAM_ERROR(400, "参数错误"),

    // ---------- 认证授权类 ----------
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "没有权限执行该操作"),
    LOGIN_ERROR(401, "账号或密码错误"),

    // ---------- 业务类 ----------
    NOT_FOUND(404, "数据不存在"),
    BIZ_ERROR(500, "业务处理失败"),

    // ---------- 系统类 ----------
    SYSTEM_ERROR(500, "系统繁忙，请稍后再试");

    private final Integer code;
    private final String msg;

    ErrorCode(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}
