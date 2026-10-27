package com.example.demo3.controller;

import com.example.demo3.annotation.RequirePermission;
import com.example.demo3.common.Result;
import com.example.demo3.dto.LoginDTO;
import com.example.demo3.entity.Permission;
import com.example.demo3.entity.SysUser;
import com.example.demo3.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 认证控制器：登录 / 注销 / 当前用户信息
 */
@Tag(name = "认证授权", description = "登录 / 注销 / 当前用户信息")
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Resource
    private AuthService authService;

    /**
     * 登录
     * POST http://localhost:8080/auth/login
     * {"username":"admin","password":"admin123"}
     * 返回：用户信息 + token + 权限码列表
     */
    @Operation(summary = "用户登录", description = "校验账号密码（BCrypt），成功返回用户信息 + Token + 权限码；连续失败 5 次锁定 15 分钟")
    @PostMapping("/login")
    public Result<SysUser> login(@Valid @RequestBody LoginDTO dto){
        SysUser user = authService.login(dto);
        return Result.success(user);
    }

    /**
     * 注销：删除 Redis 登录态
     * POST http://localhost:8080/auth/logout（带 Authorization 头）
     */
    @Operation(summary = "注销登录", description = "删除 Redis 中的 Token 登录态，需携带 Authorization: Bearer {token}")
    @PostMapping("/logout")
    public Result<String> logout(@Parameter(description = "请求头 Authorization: Bearer {token}")
                                 HttpServletRequest request){
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        authService.logout(token);
        return Result.success("已安全退出");
    }

    /**
     * 查询当前登录用户信息（含权限列表）
     * GET http://localhost:8080/auth/me（带 Authorization 头）
     */
    @Operation(summary = "当前用户信息", description = "返回当前登录用户信息与权限列表，需携带 Authorization: Bearer {token}")
    @GetMapping("/me")
    public Result<SysUser> me(@Parameter(description = "请求头 Authorization: Bearer {token}")
                              HttpServletRequest request){
        SysUser user = (SysUser) request.getAttribute("currentUser");
        if (user == null) {
            return Result.fail(401, "未登录或登录已过期");
        }
        // 刷新权限列表返回
        List<Permission> perms = authService.getPermissionsByRole(user.getRole());
        user.setPermissions(perms.stream().map(Permission::getPermCode).toList());
        return Result.success(user);
    }
}
