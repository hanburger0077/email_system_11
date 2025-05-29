package com.exmaple.backend.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class User {
    private Integer id;
    private String username;
    private String password;
    private String emailAddress;
    private String phoneNumber;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}