package com.example.demo3.controller;

import com.example.demo3.annotation.RequirePermission;
import com.example.demo3.common.PageResult;
import com.example.demo3.common.Result;
import com.example.demo3.dto.EmpQueryDTO;
import com.example.demo3.entity.Emp;
import com.example.demo3.service.EmpService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 员工控制器，处理员工相关接口
 * @RestController = @Controller + @ResponseBody
 * 作用：1.交给IOC容器管理；2.返回数据自动转为JSON（前后端分离常用）
 *
 * 接口统一用 Result/PageResult 包装，参数校验用 @Valid + 全局异常处理
 */
@RestController
@RequestMapping("/emp") // 接口统一前缀：所有接口地址前面都带上 /emp
public class EmpController {

    // 注入业务层对象
    @Resource
    private EmpService empService;

    /**
     * 接口1：根据id查询单个员工
     * 访问地址：GET http://localhost:8080/emp/1
     * @PathVariable：接收路径上的变量id
     */
    @GetMapping("/{id}")
    @RequirePermission("emp:view")
    public Result<Emp> getEmpById(@PathVariable Integer id){
        Emp emp = empService.getEmpById(id);
        return Result.success(emp);
    }

    /**
     * 接口2：查询全部员工（联查部门名称）
     * 访问地址：GET http://localhost:8080/emp/list
     */
    @GetMapping("/list")
    @RequirePermission("emp:view")
    public Result<List<Emp>> getAllEmp(){
        List<Emp> empList = empService.getAllEmp();
        return Result.success(empList);
    }

    /**
     * 接口3：条件分页查询（核心查询接口）
     * 访问地址：GET http://localhost:8080/emp/page?pageNum=1&pageSize=10&name=张&deptId=1
     * @Valid 触发 EmpQueryDTO 上的校验注解，失败由全局异常处理返回 400
     */
    @GetMapping("/page")
    @RequirePermission("emp:view")
    public Result<PageResult<Emp>> getEmpPage(@Valid EmpQueryDTO query){
        PageResult<Emp> page = empService.getEmpPage(query);
        return Result.success(page);
    }

    /**
     * 新增员工接口
     * @PostMapping：只接收POST请求
     * @RequestBody：读取请求体JSON，自动转为Emp实体对象（@Valid 字段级校验）
     * @Valid：姓名非空、年龄18~60、手机号格式，任一不满足返回字段级错误
     * 访问地址：POST http://localhost:8080/emp/add
     */
    @PostMapping("/add")
    @RequirePermission("emp:add")
    public Result<String> addEmp(@Valid @RequestBody Emp emp){
        empService.addEmp(emp);
        return Result.success("新增员工成功");
    }

    /**
     * 修改员工
     * 访问地址：PUT http://localhost:8080/emp/1
     */
    @PutMapping("/{id}")
    @RequirePermission("emp:edit")
    public Result<String> updateEmp(@PathVariable Integer id, @Valid @RequestBody Emp emp){
        emp.setId(id);
        empService.updateEmp(emp);
        return Result.success("修改员工成功");
    }

    /**
     * 删除单个员工
     * 访问地址：DELETE http://localhost:8080/emp/1
     */
    @DeleteMapping("/{id}")
    @RequirePermission("emp:delete")
    public Result<String> deleteEmp(@PathVariable Integer id){
        empService.deleteEmp(id);
        return Result.success("删除员工成功");
    }

    /**
     * 批量删除员工
     * 请求体：{"ids":[1,2,3]} 或 [1,2,3]
     * 一次 DELETE ... IN (...) 删除多条，避免前端循环调单删接口
     * 访问地址：DELETE http://localhost:8080/emp/batch
     */
    @DeleteMapping("/batch")
    @RequirePermission("emp:delete")
    public Result<String> batchDeleteEmp(@RequestBody List<Integer> ids){
        empService.batchDeleteEmp(ids);
        return Result.success("批量删除成功");
    }

}
