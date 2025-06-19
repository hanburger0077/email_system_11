package com.example.backend.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data // Lombok annotation: Automatically generates getters, setters, toString(), equals(), and hashCode() methods.
@NoArgsConstructor // Lombok annotation: Automatically generates a no-argument constructor.
@AllArgsConstructor // Lombok annotation: Automatically generates a constructor with all fields as arguments.
@JsonInclude(JsonInclude.Include.NON_NULL) //适配前端的注解
public class ResultVo<T> { // 添加了泛型 <T>**
    private String code; // 状态码，有字符串 code.ok 表示成功，字符串 code.error 表示错误等
    private String message;   // 响应消息，例如 "登录成功", "密码错误"
    private T data;       // <data字段的类型改为泛型 T
    private String reason;  //

    public static <T> ResultVo<T> success(String msg, T data) {
        return new ResultVo<>("code.ok", msg, data, null); // 假设 1001 是成功的通用码
    }

    public static <T> ResultVo<T> success(String msg) {
        return new ResultVo<>("code.ok", msg, null, null);
    }

    public static <T> ResultVo<T> fail(String msg, String reason) {
        return new ResultVo<>("code.error", msg, null, reason);
    }
}