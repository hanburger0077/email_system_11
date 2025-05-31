package com.example.backend.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class User implements Serializable {
    private Integer id;
    private String username;
    private String password;
    private String emailAddress;
    private String phoneNumber;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}