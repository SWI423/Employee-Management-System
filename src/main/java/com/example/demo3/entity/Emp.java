package com.example.demo3.entity;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.Date;

/**
 * 员工实体
 * 校验注解说明：新增/修改时 @Valid 触发，配合全局异常处理返回字段级错误
 */
@Data
public class Emp {
    private Integer id;         //员工主键

    @NotBlank(message = "员工姓名不能为空")
    private String name;        //员工姓名

    @Min(value = 18, message = "年龄必须在18~60之间")
    @Max(value = 60, message = "年龄必须在18~60之间")
    private Integer age;        //年龄

    private String gender;      //性别

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;       //手机号

    private String job;         //岗位
    private Integer deptId;     //部门id，数据库dept_id → java deptId（驼峰）
    private Date entryDate;     //入职日期
    private Date createTime;    //创建时间
    private Date updateTime;    //更新时间

    // ---------- 非数据库字段 ----------
    // 联查部门名称（emp LEFT JOIN dept），仅查询返回时使用，不参与写库
    private String deptName;
}
