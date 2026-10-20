package com.example.demo3.mapper;

import com.example.demo3.entity.Permission;
import com.example.demo3.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 系统用户Mapper：登录校验 + 权限查询
 */
@Mapper
public interface SysUserMapper {

    // 按账号查用户（登录时校验密码）
    SysUser getByUsername(String username);

    // 查某角色拥有的全部权限码（登录后 + AOP鉴权用）
    List<String> selectPermCodesByRole(@Param("role") String role);

    // 查某角色的全部权限明细（权限管理页展示）
    List<Permission> selectPermsByRole(String role);
}
