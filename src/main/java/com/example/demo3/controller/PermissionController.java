package com.example.demo3.controller;

import com.example.demo3.annotation.RequirePermission;
import com.example.demo3.common.Result;
import com.example.demo3.entity.Permission;
import com.example.demo3.service.AuthService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 权限管理控制器：查看角色权限配置
 */
@RestController
@RequestMapping("/perm")
public class PermissionController {

    @Resource
    private AuthService authService;

    /**
     * 查询某角色的全部权限
     * GET http://localhost:8080/perm/list?role=admin
     * 需要 perm:view 权限——普通用户(user)没有此权限，会被 AOP 切面拦下返回 403
     */
    @GetMapping("/list")
    @RequirePermission("perm:view")
    public Result<List<Permission>> list(@RequestParam(defaultValue = "admin") String role){
        return Result.success(authService.getPermissionsByRole(role));
    }
}
