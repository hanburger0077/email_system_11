package com.example.backend.service;


import com.example.backend.utils.ResultVo;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequest;

public interface UserService {
    //登录
    ResultVo login(String email, String password, HttpServletResponse response);

    //注册
    ResultVo register(String username, String password, String confirmPassword, String email, String phone);

    //注销 (逻辑删除账户) - 必须在登录状态下进行，通过请求获取当前用户，**并提供密码进行二次验证**
    ResultVo logout(HttpServletRequest request, String password); // **新增 password 参数**

    //退出（清除登录状态）-必须在登录状态下进行，通过请求获取当前用户
    ResultVo exit(HttpServletRequest request, HttpServletResponse response);

    //修改用户名 - 必须在登录状态下进行
    ResultVo updateUsername(HttpServletRequest request, String newUsername);

    //修改手机号 - 必须在登录状态下进行
    ResultVo updatePhone(HttpServletRequest request, String newPhone);

    //修改密码 - 在登录状态下，提供旧密码，新密码，确认密码
    ResultVo updatePassword(HttpServletRequest request, String oldPassword, String newPassword, String confirmNewPassword);

    /**
     * 通过恢复码重置密码
     * @param email 用户邮箱
     * @param recoveryCode 用户提供的恢复码
     * @param newPassword 新密码
     * @param confirmNewPassword 确认新密码
     * @return ResultVo 响应结果，成功时包含新生成的恢复码
     */
    ResultVo recoverPassword(String email, String recoveryCode, String newPassword, String confirmNewPassword);
}