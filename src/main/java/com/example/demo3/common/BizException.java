package com.example.demo3.common;

/**
 * 业务异常：所有可预期的业务错误都抛这个（如"员工不存在""无权限操作"）
 * 由 GlobalExceptionHandler 统一捕获并包装成 Result 返回，接口方法保持干净
 */
public class BizException extends RuntimeException {

    private final Integer code;

    public BizException(String msg) {
        super(msg);
        this.code = 500;
    }

    public BizException(Integer code, String msg) {
        super(msg);
        this.code = code;
    }

    public Integer getCode() {
        return code;
    }
}
