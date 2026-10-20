package com.example.demo3.config;

import com.example.demo3.interceptor.AuthInterceptor;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置：注册登录态拦截器
 * 拦截规则：
 *   /emp/**  /dept/**  /perm/**  → 需要登录
 *   /auth/login 等                → 放行
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Resource
    private AuthInterceptor authInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/emp/**", "/dept/**", "/perm/**") // 拦截业务接口
                .excludePathPatterns("/auth/login", "/auth/logout"); // 放行登录注销
    }
}
