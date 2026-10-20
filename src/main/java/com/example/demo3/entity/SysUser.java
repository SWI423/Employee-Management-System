package com.example.demo3.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.util.Date;

/**
 * 系统用户（角色）实体
 */
@Data
public class SysUser {
    private Integer id;
    private String username;    //登录账号

    @JsonIgnore                 // 序列化时忽略：密码永不出现在任何接口返回里
    private String password;    //密码
    private String name;        //真实姓名
    private String role;        //角色 admin / user
    private Date createTime;
    private Date updateTime;

    // ---------- 非数据库字段 ----------
    // 登录成功后返回给前端的 Token（前端存 localStorage，请求带 Authorization 头）
    private String token;
    // 登录成功后返回给前端的权限码列表，如 ["emp:view","dept:add"]
    private java.util.List<String> permissions;
}
