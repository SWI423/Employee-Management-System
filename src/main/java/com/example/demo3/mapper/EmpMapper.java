package com.example.demo3.mapper;

import com.example.demo3.dto.EmpQueryDTO;
import com.example.demo3.entity.Emp;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 员工Mapper接口：专门操作emp员工表
 * @Mapper 注解：告诉MyBatis，这是Mapper接口，自动生成代理对象交给IOC容器
 */
@Mapper
public interface EmpMapper {
    // 根据id查询单个员工（联查部门名称）
    Emp getEmpById(Integer id);

    // 查询全部员工，返回员工集合
    List<Emp> selectAllEmp();

    // 条件分页查询：姓名/部门/岗位/性别组合筛选，SQL在XML动态拼接
    List<Emp> selectByCondition(EmpQueryDTO query);

    // ========= 新增：插入员工 =========
    int insertEmp(Emp emp);
    // 返回值int：代表数据库受影响行数，插入成功返回1，失败返回0
    void updateEmp(Emp emp);
    void deleteEmp(Integer id);

    // 批量删除：DELETE FROM emp WHERE id IN (...)，一次SQL删多条，减少往返
    int batchDeleteEmp(@Param("ids") List<Integer> ids);

    // 统计某部门下的员工数（删部门前校验，保证数据一致性）
    int countByDeptId(Integer deptId);
}
