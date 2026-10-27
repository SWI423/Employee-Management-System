package com.example.demo3.controller;

import com.example.demo3.annotation.RequirePermission;
import com.example.demo3.common.Result;
import com.example.demo3.entity.Dept;
import com.example.demo3.service.DeptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 部门控制器
 * 接口前缀 /dept，REST 风格：GET查、POST增、PUT改、DELETE删
 */
@Tag(name = "部门管理", description = "部门增删改查，列表带员工数并走 Redis 缓存")
@RestController
@RequestMapping("/dept")
public class DeptController {

    @Resource
    private DeptService deptService;

    /**
     * 查询全部部门（带员工数，热点数据走 Redis 缓存）
     * GET http://localhost:8080/dept/list
     */
    @Operation(summary = "查询全部部门", description = "返回全部部门（含员工数），热点数据走 Redis 缓存，需 dept:view 权限")
    @GetMapping("/list")
    @RequirePermission("dept:view")
    public Result<List<Dept>> getAllDept(){
        return Result.success(deptService.getAllDept());
    }

    /**
     * 根据id查询部门
     * GET http://localhost:8080/dept/1
     */
    @Operation(summary = "按 id 查询部门", description = "查询单个部门（含员工数，走缓存），需 dept:view 权限")
    @GetMapping("/{id}")
    @RequirePermission("dept:view")
    public Result<Dept> getDeptById(@Parameter(description = "部门 id") @PathVariable Integer id){
        return Result.success(deptService.getDeptById(id));
    }

    /**
     * 新增部门
     * POST http://localhost:8080/dept/add   {"deptName":"财务部","deptDesc":"..."}
     */
    @Operation(summary = "新增部门", description = "部门名唯一校验，成功后主动失效部门列表缓存，需 dept:add 权限")
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
    @Operation(summary = "修改部门", description = "改名时校验新名称不与其他部门冲突，成功后失效相关缓存，需 dept:edit 权限")
    @PutMapping("/{id}")
    @RequirePermission("dept:edit")
    public Result<String> updateDept(@Parameter(description = "部门 id") @PathVariable Integer id, @Valid @RequestBody Dept dept){
        dept.setId(id);
        deptService.updateDept(dept);
        return Result.success("修改部门成功");
    }

    /**
     * 删除部门（部门下有员工时返回业务错误）
     * DELETE http://localhost:8080/dept/1
     */
    @Operation(summary = "删除部门", description = "部门下存在员工时返回业务错误阻止删除，成功后失效相关缓存，需 dept:delete 权限")
    @DeleteMapping("/{id}")
    @RequirePermission("dept:delete")
    public Result<String> deleteDept(@Parameter(description = "部门 id") @PathVariable Integer id){
        deptService.deleteDept(id);
        return Result.success("删除部门成功");
    }
}
