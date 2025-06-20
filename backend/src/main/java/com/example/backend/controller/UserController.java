package com.example.backend.controller;

import com.example.backend.service.UserService;
import com.example.backend.utils.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;


@RestController
@RequestMapping("/api/user")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5175", "http://localhost:5178"}, allowCredentials = "true")
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

    @PostMapping("/logout") // 注销（逻辑删除）- 现在需要密码验证
    public ResultVo logout(HttpServletRequest request, @RequestBody Map<String, String> payload) { // 接收 HttpServletRequest 和请求体
        String password = payload.get("password"); // 从请求体中获取密码
        return userService.logout(request, password);
    }

    @PostMapping("/exit") // 退出（清除登录状态）
    public ResultVo exit(HttpServletRequest request, HttpServletResponse response) {
        return userService.exit(request, response);
    }

    @PostMapping("/updateUsername")
    public ResultVo updateUsername(HttpServletRequest request, @RequestBody Map<String, String> payload) {
        String newUsername = payload.get("newUsername");
        return userService.updateUsername(request, newUsername);
    }

    @PostMapping("/updatePhone")
    public ResultVo updatePhone(HttpServletRequest request, @RequestBody Map<String, String> payload) {
        String newPhone = payload.get("newPhone");
        return userService.updatePhone(request, newPhone);
    }

    @PostMapping("/updatePassword")
    public ResultVo updatePassword(HttpServletRequest request, @RequestBody Map<String, String> payload) {
        String oldPassword = payload.get("oldPassword");
        String newPassword = payload.get("newPassword");
        String confirmNewPassword = payload.get("confirmNewPassword");
        return userService.updatePassword(request, oldPassword, newPassword, confirmNewPassword);
    }
}