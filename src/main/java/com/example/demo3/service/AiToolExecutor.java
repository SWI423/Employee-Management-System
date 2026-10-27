package com.example.demo3.service;

import com.example.demo3.dto.EmpQueryDTO;
import com.example.demo3.entity.Dept;
import com.example.demo3.entity.Emp;
import com.example.demo3.mapper.DeptMapper;
import com.example.demo3.mapper.EmpMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * AI 工具执行器：把大模型声明/调用的函数翻译成真实的数据库查询
 *
 * 大模型只负责"决定调哪个工具、传什么参数"（function calling 的第一阶段），
 * 真正查数据的是这里——保证 AI 拿到的都是真实数据，不会让模型编造员工信息。
 *
 * 已注册工具：
 *   query_emp(department?, job?, gender?, name?)   → 按条件查员工
 *   count_emp_by_dept()                            → 统计各部门人数
 */
@Slf4j
@Component
public class AiToolExecutor {

    private final EmpMapper empMapper;
    private final DeptMapper deptMapper;
    private final ObjectMapper objectMapper;

    public AiToolExecutor(EmpMapper empMapper, DeptMapper deptMapper, ObjectMapper objectMapper) {
        this.empMapper = empMapper;
        this.deptMapper = deptMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * 统一入口：按工具名分发，结果以 JSON 字符串返回（供大模型回填）
     */
    public String execute(String toolName, String argumentsJson) {
        try {
            JsonNode args = objectMapper.readTree(
                    argumentsJson == null || argumentsJson.isBlank() ? "{}" : argumentsJson);
            return switch (toolName) {
                case "query_emp" -> queryEmp(args);
                case "count_emp_by_dept" -> countEmpByDept();
                default -> "{\"error\":\"未知工具: " + toolName + "\"}";
            };
        } catch (Exception e) {
            log.error("[AI工具] 执行失败 tool={} args={}", toolName, argumentsJson, e);
            return "{\"error\":\"工具执行失败: " + e.getMessage() + "\"}";
        }
    }

    /**
     * 按条件查员工：部门名先转 deptId，再走 Mapper 条件查询（与 /emp/page 同一套 SQL）
     */
    private String queryEmp(JsonNode args) {
        EmpQueryDTO query = new EmpQueryDTO();
        query.setPageNum(1);
        query.setPageSize(100);

        // 部门名称 → 部门 id（大模型给的是名称，数据库存的是 id）
        String deptName = textOrNull(args, "department");
        if (deptName != null && !deptName.isBlank()) {
            Dept dept = deptMapper.getDeptByName(deptName.trim());
            if (dept == null) {
                return "{\"message\":\"部门[" + deptName + "]不存在，现有部门：研发部、市场部、人事部\"}";
            }
            query.setDeptId(dept.getId());
        }
        query.setJob(textOrNull(args, "job"));
        query.setGender(textOrNull(args, "gender"));
        query.setName(textOrNull(args, "name"));

        List<Emp> list = empMapper.selectByCondition(query);
        if (list == null || list.isEmpty()) {
            return "{\"message\":\"没有找到符合条件的员工\"}";
        }

        // 只返回关键字段，精简上下文（姓名/部门/岗位/性别/年龄/手机号）
        ArrayNode arr = objectMapper.createArrayNode();
        for (Emp e : list) {
            ObjectNode item = objectMapper.createObjectNode();
            item.put("name", e.getName());
            item.put("deptName", e.getDeptName());
            item.put("job", e.getJob());
            item.put("gender", e.getGender());
            item.put("age", e.getAge());
            item.put("phone", e.getPhone());
            arr.add(item);
        }
        return arr.toString();
    }

    /**
     * 统计各部门人数：直接复用部门表联查出的 empCount
     */
    private String countEmpByDept() {
        List<Dept> depts = deptMapper.selectAllDept();
        ArrayNode arr = objectMapper.createArrayNode();
        if (depts != null) {
            for (Dept d : depts) {
                ObjectNode item = objectMapper.createObjectNode();
                item.put("deptName", d.getDeptName());
                item.put("empCount", d.getEmpCount());
                arr.add(item);
            }
        }
        return arr.isEmpty() ? "{\"message\":\"暂无部门数据\"}" : arr.toString();
    }

    private String textOrNull(JsonNode node, String field) {
        JsonNode v = node.get(field);
        if (v == null || v.isNull() || v.isMissingNode()) {
            return null;
        }
        return v.asText();
    }
}
