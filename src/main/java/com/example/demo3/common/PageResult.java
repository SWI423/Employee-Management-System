package com.example.demo3.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 分页查询统一返回结构
 * 前端拿到后直接渲染分页器 + 表格数据
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {
    private Long total;        // 总记录数
    private List<T> list;      // 当前页数据
    private Integer pageNum;   // 当前页码
    private Integer pageSize;  // 每页条数
    private Integer pages;     // 总页数

    /**
     * 由 PageHelper 的 Page 对象构造（Page 本身继承了 ArrayList）
     */
    public static <T> PageResult<T> of(com.github.pagehelper.Page<T> page) {
        PageResult<T> pr = new PageResult<>();
        pr.setTotal(page.getTotal());
        pr.setList(page);
        pr.setPageNum(page.getPageNum());
        pr.setPageSize(page.getPageSize());
        pr.setPages(page.getPages());
        return pr;
    }
}
