package com.example.demo3.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 密码加密配置
 *
 * 为什么只引 spring-security-crypto 而不引整套 Spring Security：
 *   我们已有自定义的 Token 拦截器 + AOP 权限体系，不需要 Security 的过滤器链；
 *   这里只取它提供的 BCryptPasswordEncoder 工具，它是无状态的加盐哈希算法，不会触发任何自动配置。
 */
@Configuration
public class PasswordConfig {

    /**
     * BCryptPasswordEncoder：
     *   - encode(raw) 加盐哈希，同一密码每次密文不同（随机盐）
     *   - matches(raw, encoded) 自动从密文里取盐重新计算比对
     *   - 默认强度 10，计算约 100ms，慢哈希防暴力破解
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
