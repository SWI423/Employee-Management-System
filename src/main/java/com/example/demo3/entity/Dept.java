package com.example.demo3.entity;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Date;

/**
 * 部门实体
 */
@Data
public class Dept {
    private Integer id;          //部门主键

    @NotBlank(message = "部门名称不能为空")
    private String deptName;     //部门名称

    private String deptDesc;     //部门描述
    private Date createTime;     //创建时间

    // ---------- 非数据库字段 ----------
    // 部门列表联查：该部门下的员工数（前端展示 + 删除前提示）
    private Integer empCount;
}
