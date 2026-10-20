package com.example.demo3.service;

import com.example.demo3.dto.LoginDTO;
import com.example.demo3.entity.SysUser;

import java.util.List;

/**
 * 认证授权业务接口
 */
public interface AuthService {

    /**
     * 登录：校验账号密码 → 生成 Token（存 Redis）→ 返回用户信息+权限列表
     */
    SysUser login(LoginDTO dto);

    /**
     * 注销：删除 Redis 中的 Token
     */
    void logout(String token);

    /**
     * 查询某角色的全部权限明细（权限管理页）
     */
    List<com.example.demo3.entity.Permission> getPermissionsByRole(String role);
}
