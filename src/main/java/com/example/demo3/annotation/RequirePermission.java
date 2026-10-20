package com.example.demo3.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 权限校验注解：标注在 Controller 方法上，声明"调用这个接口需要什么权限"
 * 由 PermissionAspect 切面统一拦截校验，业务方法零侵入
 *
 * 用法：@RequirePermission("emp:add")
 * 校验逻辑：当前登录用户的角色 → 查 permission 表 → 是否包含该权限码
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {

    /** 权限码，如 emp:add */
    String value();
}
