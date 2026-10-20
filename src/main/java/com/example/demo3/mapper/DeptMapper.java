package com.example.demo3.mapper;

import com.example.demo3.entity.Dept;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 部门Mapper接口：操作dept部门表
 */
@Mapper
public interface DeptMapper {

    // 根据id查询部门
    Dept getDeptById(Integer id);

    // 查询全部部门（联查员工数量）
    List<Dept> selectAllDept();

    // 按名称精确查询（唯一键校验用）
    Dept getDeptByName(String deptName);

    // 新增部门
    int insertDept(Dept dept);

    // 修改部门
    int updateDept(Dept dept);

    // 删除部门
    int deleteDept(Integer id);
}
