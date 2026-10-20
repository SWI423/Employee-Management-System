package com.example.demo3.service.impl;

import com.example.demo3.common.BizException;
import com.example.demo3.common.ErrorCode;
import com.example.demo3.common.TokenStore;
import com.example.demo3.dto.LoginDTO;
import com.example.demo3.entity.Permission;
import com.example.demo3.entity.SysUser;
import com.example.demo3.mapper.SysUserMapper;
import com.example.demo3.service.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * 认证授权业务实现
 *
 * 登录态方案（课程设计版）：
 *   登录成功 → 生成 UUID Token → TokenStore 存储 token→用户（Redis 优先，故障降级内存，2小时滑动过期）
 *   → 前端后续请求带 Authorization: Bearer {token}
 * 密码说明：课程设计阶段明文存储，生产环境必须 BCrypt 加密 + 登录失败次数限制
 */
@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    private final SysUserMapper sysUserMapper;
    private final TokenStore tokenStore;

    public AuthServiceImpl(SysUserMapper sysUserMapper,
                           TokenStore tokenStore) {
        this.sysUserMapper = sysUserMapper;
        this.tokenStore = tokenStore;
    }

    @Override
    public SysUser login(LoginDTO dto) {
        // 1. 查用户
        SysUser user = sysUserMapper.getByUsername(dto.getUsername());
        if (user == null || !user.getPassword().equals(dto.getPassword())) {
            // 不区分"账号不存在"和"密码错误"，防止账号枚举攻击
            throw new BizException(ErrorCode.LOGIN_ERROR.getCode(), "账号或密码错误");
        }

        // 2. 生成 Token 并保存登录态（Redis 优先，不可用时自动降级内存存储，不阻断登录）
        String token = UUID.randomUUID().toString().replace("-", "");
        tokenStore.save(token, user);

        // 3. 附带角色权限码，前端据此做按钮级权限控制
        List<String> permCodes = sysUserMapper.selectPermCodesByRole(user.getRole());
        user.setPermissions(permCodes);

        // 4. token 放到单独字段回传（SysUser 的非数据库字段）
        user.setToken(token);
        return user;
    }

    @Override
    public void logout(String token) {
        if (token != null && !token.isBlank()) {
            tokenStore.delete(token);
        }
    }

    @Override
    public List<Permission> getPermissionsByRole(String role) {
        return sysUserMapper.selectPermsByRole(role);
    }
}
