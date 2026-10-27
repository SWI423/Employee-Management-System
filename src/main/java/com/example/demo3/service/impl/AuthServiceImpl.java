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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 认证授权业务实现
 *
 * 登录态方案：
 *   登录成功 → 生成 UUID Token → TokenStore 存储 token→用户（Redis 优先，故障降级内存，2小时滑动过期）
 *   → 前端后续请求带 Authorization: Bearer {token}
 * 密码方案：
 *   数据库存 BCrypt 加盐密文，登录用 passwordEncoder.matches(明文, 密文) 校验，不落明文
 * 防爆破方案：
 *   Redis 计数 auth:fail:{username}，连续失败 5 次锁定 15 分钟；登录成功清零
 */
@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    /** 登录失败计数 key 前缀 */
    private static final String FAIL_KEY_PREFIX = "auth:fail:";
    /** 最大允许连续失败次数 */
    private static final int MAX_FAIL = 5;
    /** 锁定时长（分钟） */
    private static final long LOCK_MINUTES = 15;

    private final SysUserMapper sysUserMapper;
    private final TokenStore tokenStore;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate stringRedisTemplate;

    public AuthServiceImpl(SysUserMapper sysUserMapper,
                           TokenStore tokenStore,
                           PasswordEncoder passwordEncoder,
                           StringRedisTemplate stringRedisTemplate) {
        this.sysUserMapper = sysUserMapper;
        this.tokenStore = tokenStore;
        this.passwordEncoder = passwordEncoder;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public SysUser login(LoginDTO dto) {
        String username = dto.getUsername();

        // 0. 先查锁定状态：失败计数 >= 5 直接拒绝（Redis 故障时降级放行，不阻断登录）
        try {
            String failKey = FAIL_KEY_PREFIX + username;
            String failStr = stringRedisTemplate.opsForValue().get(failKey);
            if (failStr != null) {
                int failCount = Integer.parseInt(failStr);
                if (failCount >= MAX_FAIL) {
                    log.warn("账号 {} 已连续失败 {} 次，处于锁定状态", username, failCount);
                    throw new BizException(ErrorCode.LOGIN_LOCKED);
                }
            }
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.warn("读取登录失败计数失败，降级放行: {}", e.getMessage());
        }

        // 1. 查用户；密码用 BCrypt matches 比对（user==null 短路，不调 matches 避免空指针）
        SysUser user = sysUserMapper.getByUsername(username);
        if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            // 登录失败：失败计数 +1（第一次失败时设置 15 分钟过期）
            recordLoginFail(username);
            // 不区分"账号不存在"和"密码错误"，防止账号枚举攻击
            throw new BizException(ErrorCode.LOGIN_ERROR);
        }

        // 2. 登录成功：清零失败计数
        clearLoginFail(username);

        // 3. 生成 Token 并保存登录态（Redis 优先，不可用时自动降级内存存储，不阻断登录）
        String token = UUID.randomUUID().toString().replace("-", "");
        tokenStore.save(token, user);

        // 4. 附带角色权限码，前端据此做按钮级权限控制
        List<String> permCodes = sysUserMapper.selectPermCodesByRole(user.getRole());
        user.setPermissions(permCodes);

        // 5. token 放到单独字段回传（SysUser 的非数据库字段）
        user.setToken(token);
        return user;
    }

    /**
     * 记录一次登录失败：INCR 计数，首次失败时设置 TTL 15 分钟
     */
    private void recordLoginFail(String username) {
        try {
            String failKey = FAIL_KEY_PREFIX + username;
            Long count = stringRedisTemplate.opsForValue().increment(failKey);
            if (count != null && count == 1) {
                stringRedisTemplate.expire(failKey, LOCK_MINUTES, TimeUnit.MINUTES);
            }
            log.info("账号 {} 登录失败，当前第 {} 次", username, count);
        } catch (Exception e) {
            log.warn("记录登录失败次数失败，忽略: {}", e.getMessage());
        }
    }

    /**
     * 登录成功：清除失败计数
     */
    private void clearLoginFail(String username) {
        try {
            stringRedisTemplate.delete(FAIL_KEY_PREFIX + username);
        } catch (Exception e) {
            log.warn("清除登录失败次数失败，忽略: {}", e.getMessage());
        }
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
