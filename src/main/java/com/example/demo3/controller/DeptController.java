package com.example.demo3.controller;

import com.example.demo3.annotation.RequirePermission;
import com.example.demo3.common.Result;
import com.example.demo3.entity.Dept;
import com.example.demo3.service.DeptService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 部门控制器
 * 接口前缀 /dept，REST 风格：GET查、POST增、PUT改、DELETE删
 */
@RestController
@RequestMapping("/dept")
public class DeptController {

    @Resource
    private DeptService deptService;

    /**
     * 查询全部部门（带员工数，热点数据走 Redis 缓存）
     * GET http://localhost:8080/dept/list
     */
    @GetMapping("/list")
    @RequirePermission("dept:view")
    public Result<List<Dept>> getAllDept(){
        return Result.success(deptService.getAllDept());
    }

    /**
     * 根据id查询部门
     * GET http://localhost:8080/dept/1
     */
    @GetMapping("/{id}")
    @RequirePermission("dept:view")
    public Result<Dept> getDeptById(@PathVariable Integer id){
        return Result.success(deptService.getDeptById(id));
    }

    /**
     * 新增部门
     * POST http://localhost:8080/dept/add   {"deptName":"财务部","deptDesc":"..."}
     */
    @PostMapping("/add")
    @RequirePermission("dept:add")
    public Result<String> addDept(@Valid @RequestBody Dept dept){
        deptService.addDept(dept);
        return Result.success("新增部门成功");
    }

    /**
     * 修改部门
     * PUT http://localhost:8080/dept/1
     */
    @PutMapping("/{id}")
    @RequirePermission("dept:edit")
    public Result<String> updateDept(@PathVariable Integer id, @Valid @RequestBody Dept dept){
        dept.setId(id);
        deptService.updateDept(dept);
        return Result.success("修改部门成功");
    }

    /**
     * 删除部门（部门下有员工时返回业务错误）
     * DELETE http://localhost:8080/dept/1
     */
    @DeleteMapping("/{id}")
    @RequirePermission("dept:delete")
    public Result<String> deleteDept(@PathVariable Integer id){
        deptService.deleteDept(id);
        return Result.success("删除部门成功");
    }
}
