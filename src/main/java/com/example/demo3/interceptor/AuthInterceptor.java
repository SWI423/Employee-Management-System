package com.example.demo3.interceptor;

import com.example.demo3.common.BizException;
import com.example.demo3.common.ErrorCode;
import com.example.demo3.common.TokenStore;
import com.example.demo3.entity.SysUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 登录态拦截器
 * 拦截业务接口（/emp /dept /perm），校验请求头里的 Token：
 *   1. 无 Token              → 401 未登录
 *   2. Token 不在 TokenStore → 401 登录已过期
 *   3. 校验通过 → 用户对象放入 request attribute，供权限切面读取
 *
 * TokenStore 内部 Redis 优先、故障自动降级内存，Redis 未安装时系统仍可正常使用。
 *
 * 放行：登录接口本身 /auth/login、静态资源、错误页
 */
@Slf4j
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final TokenStore tokenStore;

    public AuthInterceptor(TokenStore tokenStore) {
        this.tokenStore = tokenStore;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                            Object handler) throws Exception {
        // 预检请求（CORS）直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // 1. 取 Token：优先 Authorization 头（Bearer xxx），兼容 token 参数
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        if (token == null || token.isBlank()) {
            throw new BizException(ErrorCode.UNAUTHORIZED.getCode(), "未登录，请先登录");
        }

        // 2. 查登录态（TokenStore：Redis 优先，故障降级内存；查不到即过期）
        SysUser user = tokenStore.get(token);
        if (user == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED.getCode(), "登录已过期，请重新登录");
        }

        // 3. 注入当前用户，权限切面从 request attribute 读取
        request.setAttribute("currentUser", user);
        return true;
    }
}
