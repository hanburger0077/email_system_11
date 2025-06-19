package com.example.backend.controller;

import com.example.backend.service.UserService;
import com.example.backend.utils.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map; // 导入 Map 用于处理请求体

@RestController
@RequestMapping("/api/user") // 更新了基础路径以匹配 API URL 格式
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/login")
    public ResultVo login(@RequestBody Map<String, String> payload, HttpServletResponse response) {
        String email = payload.get("email");
        String password = payload.get("password");
        return userService.login(email, password, response);
    }

    @PostMapping("/register")
    public ResultVo register(@RequestBody Map<String, String> payload) {
        String username = payload.get("username");
        String password = payload.get("password");
        String confirmPassword = payload.get("confirmPassword");
        String email = payload.get("email");
        String phone = payload.get("phone");
        return userService.register(username, password, confirmPassword, email, phone);
    }

    @PostMapping("/logout")
    public ResultVo logout(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        String password = payload.get("password");
        return userService.logout(email, password);
    }

    @PostMapping("/exit")
    public ResultVo exit(HttpServletRequest request, HttpServletResponse response) {
        return userService.exit(request, response);
    }

    @PostMapping("/updateUsername")
    public ResultVo updateUsername(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        String password = payload.get("password");
        String oldUsername = payload.get("oldUsername");
        String newUsername = payload.get("newUsername");
        return userService.updateUsername(email, password, oldUsername, newUsername);
    }

    @PostMapping("/updatePhone")
    public ResultVo updatePhone(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        String password = payload.get("password");
        String oldPhone = payload.get("oldPhone");
        String newPhone = payload.get("newPhone");
        return userService.updatePhone(email, password, oldPhone, newPhone);
    }

    @PostMapping("/updatePassword")
    public ResultVo updatePassword(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        String oldPassword = payload.get("oldPassword");
        String newPassword = payload.get("newPassword");
        return userService.updatePassword(email, oldPassword, newPassword);
    }
}
