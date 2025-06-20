package com.example.backend.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data // Lombok 注解，用于自动生成 getter、setter、toString、equals 和 hashCode
@NoArgsConstructor // Lombok 注解，用于自动生成无参构造函数
@AllArgsConstructor // Lombok 注解，用于自动生成全参构造函数
public class User {
    private Long id;
    private String username;
    private String password; // 哈希后的密码
    private String email;
    private String phone;
    private Integer status; // 1 表示活跃，0 表示逻辑删除/非活跃
    private String hashedRecoveryCode; // 用于密码重置的哈希恢复码
}