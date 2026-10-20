package com.example.demo3.service.impl;

import com.example.demo3.common.BizException;
import com.example.demo3.common.CacheKeys;
import com.example.demo3.common.ErrorCode;
import com.example.demo3.common.PageResult;
import com.example.demo3.dto.EmpQueryDTO;
import com.example.demo3.entity.Dept;
import com.example.demo3.entity.Emp;
import com.example.demo3.mapper.DeptMapper;
import com.example.demo3.mapper.EmpMapper;
import com.example.demo3.service.EmpService;
import com.github.pagehelper.PageHelper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * Service实现类，实现接口里面所有方法
 * @Service：把这个类交给Spring IOC容器管理，变成Bean，Controller可注入使用
 *
 * 分层职责：Controller 收参数 → Service 写业务校验 → Mapper 执行SQL
 * 业务异常抛 BizException，由全局异常处理器统一包装，方法签名保持干净
 */
@Slf4j
@Service
public class EmpServiceImpl implements EmpService {

    // 注入Mapper对象，IOC自动帮我们创建EmpMapper代理对象
    @Resource
    private EmpMapper empMapper;

    // 注入部门Mapper：新增/修改员工时要校验部门是否存在
    @Resource
    private DeptMapper deptMapper;

    // 注入 Redis：员工增删改后联动失效部门列表缓存（部门列表联查员工数，
    // 员工变了 empCount 就过期了，不失效会出现"缓存里员工数对不上"）
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public Emp getEmpById(Integer id) {
        Emp emp = empMapper.getEmpById(id);
        if (emp == null) {
            // 404：开发时主动抛出，全局异常处理器转成 {code:404, msg:"员工不存在"}
            throw new BizException(ErrorCode.NOT_FOUND.getCode(), "员工不存在");
        }
        return emp;
    }

    @Override
    public List<Emp> getAllEmp() {
        return empMapper.selectAllEmp();
    }

    @Override
    public PageResult<Emp> getEmpPage(EmpQueryDTO query) {
        // PageHelper 分页：startPage 后面的第一个 SELECT 自动被改写为 LIMIT 分页
        // 原理：ThreadLocal 拦截 SQL，查询完成后自动清理，不影响后续查询
        PageHelper.startPage(query.getPageNum(), query.getPageSize());
        List<Emp> list = empMapper.selectByCondition(query);
        return PageResult.of((com.github.pagehelper.Page<Emp>) list);
    }

    @Override
    public void addEmp(Emp emp) {
        // ========= 业务校验【写在Service！Mapper只负责SQL，不写业务判断】=========
        // 字段格式校验（@NotBlank/@Min/@Pattern）由 @Valid + 全局异常处理负责，
        // 这里只写"跨表/需要查库"的业务规则

        // 业务规则1：部门必须存在（外键前先校验，给出人话错误而不是SQL约束报错）
        if (emp.getDeptId() != null) {
            Dept dept = deptMapper.getDeptById(emp.getDeptId());
            if (dept == null) {
                throw new BizException(ErrorCode.BIZ_ERROR.getCode(), "所选部门不存在");
            }
        }

        // 业务规则2：手机号不能重复（同一手机号只允许一个员工）
        // 简化处理：课程设计阶段跳过全表扫描，真实项目加唯一索引+捕获重复键异常

        // 补充：自动设置创建、更新时间
        emp.setCreateTime(new Date());
        emp.setUpdateTime(new Date());

        // 调用mapper插入数据库
        int rows = empMapper.insertEmp(emp);
        if (rows != 1) {
            throw new BizException(ErrorCode.BIZ_ERROR.getCode(), "新增员工失败");
        }

        // 联动失效：部门列表缓存里联查的员工数已过期
        invalidateDeptCache();
    }

    //======== 修改员工 =========
    @Override
    public void updateEmp(Emp emp) {
        // 业务校验：员工必须存在，否则报404而不是"更新0行"静默失败
        if (empMapper.getEmpById(emp.getId()) == null) {
            throw new BizException(ErrorCode.NOT_FOUND.getCode(), "员工不存在");
        }
        // 部门存在性校验
        if (emp.getDeptId() != null) {
            Dept dept = deptMapper.getDeptById(emp.getDeptId());
            if (dept == null) {
                throw new BizException(ErrorCode.BIZ_ERROR.getCode(), "所选部门不存在");
            }
        }
        // 更新时自动刷新更新时间
        emp.setUpdateTime(new Date());
        empMapper.updateEmp(emp);

        // 联动失效：员工可能换了部门，两个部门的员工数都变了
        invalidateDeptCache();
    }

    //======== 删除员工 =========
    @Override
    public void deleteEmp(Integer id) {
        if (empMapper.getEmpById(id) == null) {
            throw new BizException(ErrorCode.NOT_FOUND.getCode(), "员工不存在");
        }
        empMapper.deleteEmp(id);

        // 联动失效：部门员工数变了
        invalidateDeptCache();
    }

    //======== 批量删除 =========
    @Override
    public void batchDeleteEmp(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_ERROR.getCode(), "请选择要删除的员工");
        }
        // 单次 DELETE ... IN (...) 完成，避免循环单删的多次数据库往返
        empMapper.batchDeleteEmp(ids);

        // 联动失效：部门员工数变了
        invalidateDeptCache();
    }

    /**
     * 失效部门列表缓存：员工增删改后，dept:list 里联查的 empCount 已过期
     * 精确 key（dept:{id}）不删——员工变动不影响部门名称/描述本身
     */
    private void invalidateDeptCache() {
        try {
            redisTemplate.delete(CacheKeys.DEPT_LIST);
        } catch (Exception e) {
            // 失效失败不阻断业务：TTL 到期后自动过期，不会长期脏读
            log.warn("[缓存异常-联动失效] 依赖TTL兜底：{}", e.getMessage());
        }
    }

}
