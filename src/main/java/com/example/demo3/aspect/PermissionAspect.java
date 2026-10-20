package com.example.demo3.aspect;

import com.example.demo3.annotation.RequirePermission;
import com.example.demo3.common.BizException;
import com.example.demo3.common.ErrorCode;
import com.example.demo3.entity.SysUser;
import com.example.demo3.mapper.SysUserMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 权限校验切面
 * 拦截所有带 @RequirePermission 的方法：
 *   1. 从请求上下文拿登录用户（AuthInterceptor 已校验登录态并注入）
 *   2. 查该角色的权限码列表（Redis 缓存 + DB 回源）
 *   3. 不含所需权限码 → 抛 BizException(403)，由全局异常处理统一返回
 */
@Slf4j
@Aspect
@Component
public class PermissionAspect {

    // 角色权限码缓存：perm:role:{role}，5分钟TTL
    private static final String PERM_KEY_PREFIX = "perm:role:";
    private static final long PERM_CACHE_TTL = 5;

    private final RedisTemplate<String, Object> redisTemplate;
    private final SysUserMapper sysUserMapper;

    // 构造器注入：PermissionAspect 与 SysUserMapper 都是普通 Bean，无循环依赖
    public PermissionAspect(RedisTemplate<String, Object> redisTemplate,
                            SysUserMapper sysUserMapper) {
        this.redisTemplate = redisTemplate;
        this.sysUserMapper = sysUserMapper;
    }

    @Around("@annotation(requirePermission)")
    public Object checkPermission(ProceedingJoinPoint point, RequirePermission requirePermission) throws Throwable {
        String required = requirePermission.value();

        // 1. 拿当前登录用户（AuthInterceptor 校验登录态后放入 request attribute）
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED.getCode(), "请求上下文缺失");
        }
        HttpServletRequest request = attrs.getRequest();
        SysUser user = (SysUser) request.getAttribute("currentUser");
        if (user == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED.getCode(), "未登录或登录已过期");
        }

        // 2. 查角色权限码（先缓存后DB）
        List<String> permCodes = getPermCodesByRole(user.getRole());
        log.debug("[鉴权] 用户={} 角色={} 需要={} 拥有={}",
                user.getUsername(), user.getRole(), required, permCodes);

        // 3. 校验
        if (!permCodes.contains(required)) {
            throw new BizException(ErrorCode.FORBIDDEN.getCode(), "没有权限执行该操作");
        }

        return point.proceed();
    }

    /**
     * 角色权限码查询：Redis 缓存 + DB 回源，Redis 故障自动降级
     */
    @SuppressWarnings("unchecked")
    private List<String> getPermCodesByRole(String role) {
        String key = PERM_KEY_PREFIX + role;
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                return (List<String>) cached;
            }
        } catch (Exception e) {
            log.warn("[缓存异常-权限读取] 降级查库：{}", e.getMessage());
        }

        List<String> codes = sysUserMapper.selectPermCodesByRole(role);
        try {
            redisTemplate.opsForValue().set(key, codes, PERM_CACHE_TTL, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("[缓存异常-权限写入] {}", e.getMessage());
        }
        return codes;
    }
}
