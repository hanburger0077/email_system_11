package com.example.backend.service;

import com.example.backend.utils.ResultVo;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequest; // 新增导入

public interface UserService {
    // 登录
    ResultVo login(String email, String password, HttpServletResponse response);

    // 注册
    ResultVo register(String username, String password, String confirmPassword, String email, String phone);

    // 注销 (删除账户)
    ResultVo logout(String email, String password);

    // 退出（清除登录状态）- 修改了方法签名
    ResultVo exit(HttpServletRequest request, HttpServletResponse response);

    // 修改用户名
    ResultVo updateUsername(String email, String password, String oldUsername, String newUsername);

    // 修改手机号
    ResultVo updatePhone(String email, String password, String oldPhone, String newPhone);

    // 修改密码
    ResultVo updatePassword(String email, String oldPassword, String newPassword);
}