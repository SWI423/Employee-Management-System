package com.example.demo3.controller;

import com.example.demo3.annotation.RequirePermission;
import com.example.demo3.common.Result;
import com.example.demo3.entity.Permission;
import com.example.demo3.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 权限管理控制器：查看角色权限配置
 */
@Tag(name = "权限管理", description = "查询角色权限配置，普通用户无 perm:view 权限会被 AOP 拦下")
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
    @Operation(summary = "查询角色权限列表", description = "按角色查询全部权限明细，需 perm:view 权限")
    @GetMapping("/list")
    @RequirePermission("perm:view")
    public Result<List<Permission>> list(@Parameter(description = "角色：admin / user，默认 admin")
                                         @RequestParam(defaultValue = "admin") String role){
        return Result.success(authService.getPermissionsByRole(role));
    }
}
