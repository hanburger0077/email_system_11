package com.example.backend.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResultVo<T> {
    private String code;    // 状态码
    private String message; // 响应消息
    private T data;
    private String reason;  // 错误具体原因

    // 带有数据的成功响应
    private ResultVo(String code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.reason = null; // 成功响应没有 reason 字段
    }

    // 带有具体原因的失败响应（接受三个参数：code, message, reason）
    // 这个构造函数现在可以被 ResultVo.fail(code, message, reason) 调用
    private ResultVo(String code, String message, String reason) {
        this.code = code;
        this.message = message;
        this.reason = reason;
        this.data = null; // 失败响应没有 data 字段
    }

    public static <T> ResultVo<T> success(String message, T data) {
        return new ResultVo<>("code.ok", message, data);
    }

    public static <T> ResultVo<T> success(String message) {
        return new ResultVo<>("code.ok", message, null);
    }


    // 并且内部固定 code 为 "code.error"，以符合您的实际调用方式
    public static <T> ResultVo<T> fail(String message, String reason) {
        return new ResultVo<>("code.error", message, reason);
    }

    // ***如果希望更灵活，也可以提供一个接受三个 String 参数的公共静态方法来匹配您的调用***
    // 例如，如果您想让调用者直接指定 code、message 和 reason：
    public static <T> ResultVo<T> fail(String code, String message, String reason) {
        return new ResultVo<>(code, message, reason);
    }
}