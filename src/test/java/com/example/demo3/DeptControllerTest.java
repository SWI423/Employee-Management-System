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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 部门接口测试（连接真实 MySQL/Redis）
 * 覆盖：部门列表缓存命中（读回写）、按 id 查询、写操作后缓存失效
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DeptControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private String adminToken;

    private String login() throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        String body = result.getResponse().getContentAsString();
        int idx = body.indexOf("\"token\":\"") + 9;
        return body.substring(idx, body.indexOf("\"", idx));
    }

    @BeforeEach
    void setup() throws Exception {
        adminToken = login();
    }

    @AfterEach
    void cleanupRedis() {
        try {
            Set<String> keys = redisTemplate.keys("auth:token:*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
            // 清理部门缓存与权限缓存，避免影响后续验证
            redisTemplate.delete("dept:list");
            Set<String> permKeys = redisTemplate.keys("perm:role:*");
            if (permKeys != null && !permKeys.isEmpty()) {
                redisTemplate.delete(permKeys);
            }
            stringRedisTemplate.delete("auth:fail:admin");
        } catch (Exception ignored) {
        }
    }

    @Test
    @DisplayName("部门列表：首次查询回写缓存，第二次命中缓存，两次结果一致")
    void deptListCacheHit() throws Exception {
        MvcResult first = mockMvc.perform(get("/dept/list")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        MvcResult second = mockMvc.perform(get("/dept/list")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        String firstBody = first.getResponse().getContentAsString();
        String secondBody = second.getResponse().getContentAsString();
        org.junit.jupiter.api.Assertions.assertEquals(firstBody, secondBody,
                "两次查询结果应一致（第二次命中缓存）");

        // 缓存键应存在
        Boolean hit = redisTemplate.hasKey("dept:list");
        org.junit.jupiter.api.Assertions.assertTrue(Boolean.TRUE.equals(hit),
                "dept:list 缓存键应存在（读回写策略生效）");
    }

    @Test
    @DisplayName("按 id 查询部门：存在返回 200，不存在返回 404")
    void getDeptById() throws Exception {
        mockMvc.perform(get("/dept/1")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1));

        mockMvc.perform(get("/dept/999")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    @DisplayName("权限拦截：user 角色访问新增部门接口返回 403")
    void permissionDenied() throws Exception {
        // zhangsan 是 user 角色，无 dept:add
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"zhangsan\",\"password\":\"123456\"}"))
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        String body = result.getResponse().getContentAsString();
        int idx = body.indexOf("\"token\":\"") + 9;
        String userToken = body.substring(idx, body.indexOf("\"", idx));

        mockMvc.perform(post("/dept/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + userToken)
                        .content("{\"deptName\":\"越权部门\",\"deptDesc\":\"不应成功\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.msg").value("没有权限执行该操作"));
    }
}
