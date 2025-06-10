package com.example.backend.utils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data // Lombok annotation: Automatically generates getters, setters, toString(), equals(), and hashCode() methods.
@NoArgsConstructor // Lombok annotation: Automatically generates a no-argument constructor.
@AllArgsConstructor // Lombok annotation: Automatically generates a constructor with all fields as arguments.
public class ResultVo<T> { // 添加了泛型 <T>**
    private Integer code; // 状态码，例如 1001 表示成功，1002 表示密码错误等
    private String msg;   // 响应消息，例如 "登录成功", "密码错误"
    private T data;       // <data字段的类型改为泛型 T

    public static <T> ResultVo<T> success(String msg, T data) {
        return new ResultVo<>(1001, msg, data); // 假设 1001 是成功的通用码
    }

    public static <T> ResultVo<T> success(String msg) {
        return new ResultVo<>(1001, msg, null);
    }

    public static <T> ResultVo<T> fail(Integer code, String msg) {
        return new ResultVo<>(code, msg, null);
    }
}