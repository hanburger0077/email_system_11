package com.example.backend.service;

import com.example.backend.utils.ResultVo;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequest;

public interface UserService {
    // 登录
    ResultVo login(String email, String password, HttpServletResponse response);

    // 注册
    ResultVo register(String username, String password, String confirmPassword, String email, String phone);

    // 注销 (逻辑删除账户) - 必须在登录状态下进行，并通过密码验证
    ResultVo logout(HttpServletRequest request, String password);

    // 退出（清除登录状态）- 必须在登录状态下进行，通过请求获取当前用户
    ResultVo exit(HttpServletRequest request, HttpServletResponse response);

    // 修改用户名 - 必须在登录状态下进行
    ResultVo updateUsername(HttpServletRequest request, String newUsername);

    // 修改手机号 - 必须在登录状态下进行
    ResultVo updatePhone(HttpServletRequest request, String newPhone);

    // 修改密码 - 必须在登录状态下进行，只通过旧密码、新密码和确认新密码验证
    ResultVo updatePassword(HttpServletRequest request, String oldPassword, String newPassword, String confirmNewPassword);
}