package com.example.backend.controller;
import com.example.backend.service.UserService;
import com.example.backend.utils.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequest; // 新增导入
@RestController
@RequestMapping("/user")
public class UserController {
    @Autowired
    private UserService userService;
    @PostMapping("/login")
    public ResultVo login(@RequestParam String email, @RequestParam String password, HttpServletResponse response) {
        return userService.login(email, password, response);
    }
    @PostMapping("/register")
    public ResultVo register(@RequestParam String username,
                             @RequestParam String password,
                             @RequestParam String confirmPassword,
                             @RequestParam String email,
                             @RequestParam String phone) {
        return userService.register(username, password, confirmPassword, email, phone);
    }
    @PostMapping("/logout")
    public ResultVo logout(@RequestParam String email, @RequestParam String password) {
        return userService.logout(email, password);
    }
    @PostMapping("/exit")
    public ResultVo exit(HttpServletRequest request, HttpServletResponse response) { // 修改了方法签名
        return userService.exit(request, response);
    }
    @PostMapping("/updateUsername")
    public ResultVo updateUsername(@RequestParam String email,
                                   @RequestParam String password,
                                   @RequestParam String oldUsername,
                                   @RequestParam String newUsername) {
        return userService.updateUsername(email, password, oldUsername, newUsername);
    }
    @PostMapping("/updatePhone")
    public ResultVo updatePhone(@RequestParam String email,
                                @RequestParam String password,
                                @RequestParam String oldPhone,
                                @RequestParam String newPhone) {
        return userService.updatePhone(email, password, oldPhone, newPhone);
    }
    @PostMapping("/updatePassword")
    public ResultVo updatePassword(@RequestParam String email,
                                   @RequestParam String oldPassword,
                                   @RequestParam String newPassword) {
        return userService.updatePassword(email, oldPassword, newPassword);
    }
}