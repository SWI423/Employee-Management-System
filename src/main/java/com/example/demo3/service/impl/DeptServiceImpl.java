package com.example.demo3.service.impl;

import com.example.demo3.common.BizException;
import com.example.demo3.common.CacheKeys;
import com.example.demo3.common.ErrorCode;
import com.example.demo3.entity.Dept;
import com.example.demo3.mapper.DeptMapper;
import com.example.demo3.mapper.EmpMapper;
import com.example.demo3.service.DeptService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 部门业务实现
 *
 * 【Redis 缓存策略】——热点部门数据三级保障：
 *  1. 读：先查缓存，命中直接返回，不打到 MySQL；
 *     未命中查库，写入缓存并设 TTL
 *  2. 写：增/改/删后【主动失效】相关缓存（精确 key + 列表 key 一起删），
 *     下次读请求自然回源重建，保证缓存与数据库一致
 *  3. 兜底：即使主动失效漏删，TTL（30分钟）到期自动过期
 *  4. 降级：Redis 故障（宕机/网络超时）时所有缓存操作 try-catch 吞掉，
 *     自动走数据库——缓存是加速件，不是单点
 */
@Slf4j
@Service
public class DeptServiceImpl implements DeptService {

    @Resource
    private DeptMapper deptMapper;

    @Resource
    private EmpMapper empMapper; // 删除部门前统计员工数

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    // ---------- 缓存键规范：模块:内容[:参数]（统一定义在 CacheKeys） ----------
    private static final long DEPT_CACHE_TTL = 30;             // 缓存30分钟

    @Override
    public List<Dept> getAllDept() {
        // 1. 先读缓存
        try {
            Object cached = redisTemplate.opsForValue().get(CacheKeys.DEPT_LIST);
            if (cached != null) {
                log.debug("[缓存命中] {}", CacheKeys.DEPT_LIST);
                @SuppressWarnings("unchecked")
                List<Dept> list = (List<Dept>) cached;
                return list;
            }
        } catch (Exception e) {
            // Redis 故障不阻断业务，降级查库
            log.warn("[缓存异常-读取] 降级查库：{}", e.getMessage());
        }

        // 2. 缓存未命中，查数据库
        List<Dept> list = deptMapper.selectAllDept();

        // 3. 回写缓存 + TTL 兜底过期
        try {
            redisTemplate.opsForValue().set(CacheKeys.DEPT_LIST, list, DEPT_CACHE_TTL, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("[缓存异常-写入] 不影响返回：{}", e.getMessage());
        }
        return list;
    }

    @Override
    public Dept getDeptById(Integer id) {
        String key = CacheKeys.DEPT_PREFIX + id;

        // 1. 先读缓存
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                log.debug("[缓存命中] {}", key);
                return (Dept) cached;
            }
        } catch (Exception e) {
            log.warn("[缓存异常-读取] 降级查库：{}", e.getMessage());
        }

        // 2. 查库
        Dept dept = deptMapper.getDeptById(id);
        if (dept == null) {
            throw new BizException(ErrorCode.NOT_FOUND.getCode(), "部门不存在");
        }

        // 3. 回写缓存
        try {
            redisTemplate.opsForValue().set(key, dept, DEPT_CACHE_TTL, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("[缓存异常-写入] 不影响返回：{}", e.getMessage());
        }
        return dept;
    }

    @Override
    public void addDept(Dept dept) {
        // 业务规则：部门名唯一（与 schema 的 uk_dept_name 双保险，提前给出友好错误）
        if (deptMapper.getDeptByName(dept.getDeptName()) != null) {
            throw new BizException(ErrorCode.BIZ_ERROR.getCode(), "部门名称已存在");
        }
        dept.setCreateTime(new Date());
        deptMapper.insertDept(dept);

        // 写操作：主动失效列表缓存，下次读取时回源拿到新部门
        invalidateDeptCache(null);
    }

    @Override
    public void updateDept(Dept dept) {
        Dept exist = deptMapper.getDeptById(dept.getId());
        if (exist == null) {
            throw new BizException(ErrorCode.NOT_FOUND.getCode(), "部门不存在");
        }
        // 改名时校验新名称不与别的部门冲突
        if (!exist.getDeptName().equals(dept.getDeptName())
                && deptMapper.getDeptByName(dept.getDeptName()) != null) {
            throw new BizException(ErrorCode.BIZ_ERROR.getCode(), "部门名称已存在");
        }
        deptMapper.updateDept(dept);

        // 写操作：同时失效列表和该部门的精确缓存
        invalidateDeptCache(dept.getId());
    }

    @Override
    public void deleteDept(Integer id) {
        Dept exist = deptMapper.getDeptById(id);
        if (exist == null) {
            throw new BizException(ErrorCode.NOT_FOUND.getCode(), "部门不存在");
        }
        // 数据一致性：部门下有员工时禁止删除（外键约束前的业务拦截，
        // 否则MySQL会抛外键异常，用户看到的是500而不是人话）
        int empCount = empMapper.countByDeptId(id);
        if (empCount > 0) {
            throw new BizException(ErrorCode.BIZ_ERROR.getCode(),
                    "该部门下还有 " + empCount + " 名员工，不能删除");
        }
        deptMapper.deleteDept(id);

        // 写操作：失效缓存
        invalidateDeptCache(id);
    }

    /**
     * 主动失效部门缓存：列表 key 全删 + 传入的精确 key 删除
     * @param id 被操作的部门id；新增时传 null（没有精确key可删，只清列表）
     */
    private void invalidateDeptCache(Integer id) {
        try {
            redisTemplate.delete(CacheKeys.DEPT_LIST);
            if (id != null) {
                redisTemplate.delete(CacheKeys.DEPT_PREFIX + id);
            }
            log.debug("[缓存失效] dept:list{}}", id != null ? " + dept:" + id : "");
        } catch (Exception e) {
            // 失效失败不阻断写业务：TTL 到期后缓存仍会自动过期，不会长期脏读
            log.warn("[缓存异常-失效] 依赖TTL兜底：{}", e.getMessage());
        }
    }
}
