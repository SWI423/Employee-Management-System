package com.example.demo3.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 员工条件查询参数（分页 + 筛选）
 * GET /emp/page?pageNum=1&pageSize=10&name=张&deptId=1&job=工程师&gender=男
 */
@Data
public class EmpQueryDTO {

    @Min(value = 1, message = "页码不能小于1")
    private Integer pageNum = 1;   //当前页，默认第1页

    @Min(value = 1, message = "每页条数不能小于1")
    private Integer pageSize = 10; //每页条数，默认10条

    private String name;    //姓名（模糊查询）
    private Integer deptId; //部门id（精确匹配）
    private String job;     //岗位（模糊查询）
    private String gender;  //性别
}
