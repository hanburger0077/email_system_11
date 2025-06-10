package com.example.backend.entity;

import lombok.Data;

@Data
public class User {
    private Integer id;
    private String username;
    private String password;
    private String email; // 新增邮箱地址字段
    private String phone;  // 新增手机号字段
}