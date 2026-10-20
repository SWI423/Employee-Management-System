package com.example.demo3.common;

import lombok.Data;

/**
 * 统一返回结果，给前端的通用JSON格式
 * 格式：{"code":200,"msg":"操作成功","data":{...}}
 */
@Data
public class Result<T> {
    private Integer code; //响应码 200成功，500失败
    private String msg;   //提示信息
    private T data;       //返回的数据，泛型，可以是Emp、List<Emp>

    //成功静态方法（带数据）
    public static <T> Result<T> success(T data){
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMsg("操作成功");
        result.setData(data);
        return result;
    }

    //成功静态方法（无数据，如删除、修改）
    public static <T> Result<T> success(){
        return success(null);
    }

    //失败静态方法（默认500）
    public static <T> Result<T> fail(String msg){
        return fail(500, msg);
    }

    //失败静态方法（自定义错误码，如401未登录、403无权限）
    public static <T> Result<T> fail(Integer code, String msg){
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMsg(msg);
        return result;
    }
}
