package com.example.backend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.example.backend.mapper") // 自动扫描Mapper接口
public class MyprojectApplication {

    public static void main(String[] args) {
        SpringApplication.run( MyprojectApplication.class, args);
    }
}