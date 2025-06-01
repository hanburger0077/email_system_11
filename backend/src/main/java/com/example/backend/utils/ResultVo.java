package com.example.backend.utils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor

public class ResultVo {
    private int code;//状态码
    private String msg;//消息提示
    private Object data;

}
