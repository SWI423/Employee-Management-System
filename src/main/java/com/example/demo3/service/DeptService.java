package com.example.demo3.service;

import com.example.demo3.entity.Dept;

import java.util.List;

/**
 * 部门业务接口
 */
public interface DeptService {

    // 查询全部部门（带员工数，热点数据走 Redis 缓存）
    List<Dept> getAllDept();

    // 根据id查询部门（走缓存）
    Dept getDeptById(Integer id);

    // 新增部门（缓存主动失效）
    void addDept(Dept dept);

    // 修改部门（缓存主动失效）
    void updateDept(Dept dept);

    // 删除部门（有员工的部门禁止删，缓存主动失效）
    void deleteDept(Integer id);
}
