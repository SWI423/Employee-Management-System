package com.example.demo3.common;

/**
 * 缓存键规范：全系统缓存 key 统一在这里定义，避免魔法字符串散落各处
 * 命名规则：模块:内容[:参数]
 */
public final class CacheKeys {

    private CacheKeys() {}

    /** 全量部门列表（热点，联查员工数） */
    public static final String DEPT_LIST = "dept:list";

    /** 单个部门，前缀 + id，如 dept:1 */
    public static final String DEPT_PREFIX = "dept:";

    /** 角色权限码，前缀 + role，如 perm:role:admin */
    public static final String PERM_ROLE_PREFIX = "perm:role:";

    /** 登录 Token，前缀 + token，如 auth:token:xxx */
    public static final String AUTH_TOKEN_PREFIX = "auth:token:";
}
