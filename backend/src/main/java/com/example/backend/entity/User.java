package com.example.backend.entity;

import lombok.Data;

@Data
public class User {
    private Integer id;
    private String username;
    private String password;
    private String email;
    private String phone; // 新增手机号字段
    private Integer status; // 新增用户状态字段：1有效，0逻辑删除
}