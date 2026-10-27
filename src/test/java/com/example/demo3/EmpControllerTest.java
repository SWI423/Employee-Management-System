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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 员工接口测试（连接真实 MySQL/Redis，数据库操作 @Transactional 回滚）
 * 覆盖：分页查询 / 新增 / 修改 / 删除 / 权限拦截 403 / 未登录 401
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EmpControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private String adminToken;
    private String userToken;

    /** 登录并返回 token（测试辅助方法） */
    private String login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        String body = result.getResponse().getContentAsString();
        // 从 JSON 中取 token（org.json 在 test 依赖里可用，直接用简单字符串截取更稳）
        int idx = body.indexOf("\"token\":\"") + 9;
        return body.substring(idx, body.indexOf("\"", idx));
    }

    @BeforeEach
    void setup() throws Exception {
        adminToken = login("admin", "admin123");
        userToken = login("zhangsan", "123456");
    }

    @AfterEach
    void cleanupRedis() {
        try {
            Set<String> keys = redisTemplate.keys("auth:token:*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
            stringRedisTemplate.delete("auth:fail:admin");
            stringRedisTemplate.delete("auth:fail:zhangsan");
        } catch (Exception ignored) {
        }
    }

    private static String auth(String token) {
        return "Bearer " + token;
    }

    @Test
    @DisplayName("分页查询：admin 可查，返回统一分页结构")
    void pageQuery() throws Exception {
        mockMvc.perform(get("/emp/page")
                        .param("pageNum", "1")
                        .param("pageSize", "5")
                        .header("Authorization", auth(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").isNumber())
                .andExpect(jsonPath("$.data.list").isArray());
    }

    @Test
    @DisplayName("条件查询：按姓名筛选")
    void pageQueryByName() throws Exception {
        mockMvc.perform(get("/emp/page")
                        .param("pageNum", "1")
                        .param("pageSize", "10")
                        .param("name", "张")
                        .header("Authorization", auth(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("新增员工：字段校验通过则入库（事务回滚）")
    void addEmp() throws Exception {
        mockMvc.perform(post("/emp/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", auth(adminToken))
                        .content("{\"name\":\"测试新增\",\"age\":25,\"gender\":\"男\",\"phone\":\"13800138000\",\"job\":\"测试岗\",\"deptId\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.msg").value("操作成功"))
                .andExpect(jsonPath("$.data").value("新增员工成功"));

        // 回滚前能在分页中查到
        mockMvc.perform(get("/emp/page")
                        .param("pageNum", "1")
                        .param("pageSize", "10")
                        .param("name", "测试新增")
                        .header("Authorization", auth(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    @DisplayName("新增员工：手机号格式非法返回 400")
    void addEmpInvalidPhone() throws Exception {
        mockMvc.perform(post("/emp/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", auth(adminToken))
                        .content("{\"name\":\"测试新增\",\"age\":25,\"phone\":\"12345\",\"deptId\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value(org.hamcrest.Matchers.containsString("手机号格式不正确")));
    }

    @Test
    @DisplayName("修改员工：admin 可改，改后查询验证（事务回滚）")
    void updateEmp() throws Exception {
        // 取一个真实员工 id：查分页第一页第一条
        MvcResult page = mockMvc.perform(get("/emp/page")
                        .param("pageNum", "1")
                        .param("pageSize", "1")
                        .header("Authorization", auth(adminToken)))
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        String body = page.getResponse().getContentAsString();
        int idIdx = body.indexOf("\"id\":") + 5;
        String id = body.substring(idIdx, body.indexOf(",", idIdx));

        mockMvc.perform(put("/emp/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", auth(adminToken))
                        .content("{\"name\":\"改名验证\",\"age\":30,\"phone\":\"13900139000\",\"deptId\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(get("/emp/" + id)
                        .header("Authorization", auth(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("改名验证"));
    }

    @Test
    @DisplayName("删除员工：删除后查询返回 404（事务回滚）")
    void deleteEmp() throws Exception {
        // 先新增一条，拿到分页里的 id 后删除
        mockMvc.perform(post("/emp/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", auth(adminToken))
                        .content("{\"name\":\"待删除\",\"age\":22,\"phone\":\"13700137000\",\"job\":\"临时\",\"deptId\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        MvcResult page = mockMvc.perform(get("/emp/page")
                        .param("pageNum", "1")
                        .param("pageSize", "10")
                        .param("name", "待删除")
                        .header("Authorization", auth(adminToken)))
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        String body = page.getResponse().getContentAsString();
        int idIdx = body.indexOf("\"id\":") + 5;
        String id = body.substring(idIdx, body.indexOf(",", idIdx));

        mockMvc.perform(delete("/emp/" + id)
                        .header("Authorization", auth(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(get("/emp/" + id)
                        .header("Authorization", auth(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    @DisplayName("权限拦截：user 角色调用新增接口返回 403（AOP）")
    void permissionDenied() throws Exception {
        mockMvc.perform(post("/emp/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", auth(userToken))
                        .content("{\"name\":\"无权限\",\"age\":25,\"phone\":\"13600136000\",\"deptId\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.msg").value("没有权限执行该操作"));
    }

    @Test
    @DisplayName("未登录访问员工接口：401")
    void unauthorized() throws Exception {
        mockMvc.perform(get("/emp/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }
}
