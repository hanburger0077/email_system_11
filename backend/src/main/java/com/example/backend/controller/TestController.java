package com.example.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class TestController {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @GetMapping("/test")
    public Map<String, Object> test() {
        Map<String, Object> result = new HashMap<>();
        result.put("message", "API服务运行正常");
        result.put("timestamp", System.currentTimeMillis());
        result.put("code", "code.ok");
        return result;
    }
    
    @GetMapping("/test/database")
    public Map<String, Object> testDatabase() {
        Map<String, Object> result = new HashMap<>();
        try {
            // 测试数据库连接并查询用户数据
            List<Map<String, Object>> users = jdbcTemplate.queryForList("SELECT id, username, email, phone, created_at FROM users");
            result.put("message", "数据库连接正常");
            result.put("userCount", users.size());
            result.put("users", users);
            result.put("code", "code.ok");
        } catch (Exception e) {
            result.put("message", "数据库连接失败: " + e.getMessage());
            result.put("code", "code.error");
        }
        return result;
    }
    
    @GetMapping("/test/security")
    public Map<String, Object> testSecurity() {
        Map<String, Object> result = new HashMap<>();
        result.put("message", "Spring Security已禁用，API可以正常访问");
        result.put("code", "code.ok");
        return result;
    }
} 