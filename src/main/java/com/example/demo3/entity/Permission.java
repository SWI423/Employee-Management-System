package com.example.demo3.entity;

import lombok.Data;

/**
 * 角色权限实体：一个角色一行权限，如 admin 持有 emp:add
 */
@Data
public class Permission {
    private Integer id;
    private String role;       //角色：admin / user
    private String permCode;   //权限码：emp:view / dept:add ...
    private String permName;   //权限名称：查看员工 / 新增部门 ...
}
