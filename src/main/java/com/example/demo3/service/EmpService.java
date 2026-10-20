package com.example.demo3.service;

import com.example.demo3.common.PageResult;
import com.example.demo3.dto.EmpQueryDTO;
import com.example.demo3.entity.Emp;

import java.util.List;

/**
 * 员工业务接口，定义业务功能
 */
public interface EmpService {
    // 根据id查询单个员工（联查部门名称）
    Emp getEmpById(Integer id);

    // 查询全部员工
    List<Emp> getAllEmp();

    // 条件分页查询（姓名/部门/岗位/性别 + 分页）
    PageResult<Emp> getEmpPage(EmpQueryDTO query);

    // 新增员工业务方法
    void addEmp(Emp emp);

    // 修改
    void updateEmp(Emp emp);

    // 删除
    void deleteEmp(Integer id);

    // 批量删除
    void batchDeleteEmp(List<Integer> ids);
}
