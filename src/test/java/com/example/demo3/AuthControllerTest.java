package com.example.demo3;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 认证接口测试（连接真实 MySQL/Redis，数据库操作 @Transactional 回滚）
 * 覆盖：BCrypt 登录成功 / 密码错误 / 失败锁定 / 锁定后正确密码也拒绝 / 无Token 401
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @BeforeEach
    void clearFailCount() {
        // 保证锁定测试从 0 计数开始
        stringRedisTemplate.delete("auth:fail:zhangsan");
    }

    @AfterEach
    void cleanupRedis() {
        // 测试产生的 Token 与失败计数清理，避免污染其他测试
        try {
            Set<String> keys = redisTemplate.keys("auth:token:*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
            stringRedisTemplate.delete("auth:fail:admin");
            stringRedisTemplate.delete("auth:fail:zhangsan");
        } catch (Exception ignored) {
            // Redis 故障不影响测试结论
        }
    }

    @Test
    @DisplayName("登录成功：admin/admin123 返回 token 与权限码")
    void loginSuccess() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.username").value("admin"))
                .andExpect(jsonPath("$.data.role").value("admin"))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.permissions").isArray());
    }

    @Test
    @DisplayName("密码错误：返回 401 且不暴露具体原因（防账号枚举）")
    void loginWrongPassword() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.msg").value(org.hamcrest.Matchers.containsString("账号或密码错误")));
    }

    @Test
    @DisplayName("账号不存在：与密码错误同文案（防枚举）")
    void loginUserNotExists() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"nobody\",\"password\":\"whatever\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.msg").value(org.hamcrest.Matchers.containsString("账号或密码错误")));
    }

    @Test
    @DisplayName("连续失败 5 次锁定：第 6 次即使密码正确也被拒（Redis 计数）")
    void loginLockAfterFiveFails() throws Exception {
        // 前 5 次错误密码
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"zhangsan\",\"password\":\"bad\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(401));
        }

        // 验证 Redis 计数 = 5
        String count = stringRedisTemplate.opsForValue().get("auth:fail:zhangsan");
        org.junit.jupiter.api.Assertions.assertEquals("5", count, "失败计数应累计到 5");

        // 第 6 次：正确密码也被锁定
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"zhangsan\",\"password\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.msg").value(org.hamcrest.Matchers.containsString("锁定")));
    }

    @Test
    @DisplayName("登录成功后失败计数清零：锁解除可正常登录")
    void loginSuccessClearsFailCount() throws Exception {
        // 先错 3 次
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"zhangsan\",\"password\":\"bad\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(401));
        }
        // 成功登录一次 → 计数清零
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"zhangsan\",\"password\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        String count = stringRedisTemplate.opsForValue().get("auth:fail:zhangsan");
        org.junit.jupiter.api.Assertions.assertNull(count, "登录成功后失败计数应被清除");
    }

    @Test
    @DisplayName("无 Token 访问业务接口：401 未登录")
    void accessWithoutToken() throws Exception {
        mockMvc.perform(get("/emp/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }
}
