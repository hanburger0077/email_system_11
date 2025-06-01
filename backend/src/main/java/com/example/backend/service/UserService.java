package com.example.backend.service;

import com.example.backend.utils.ResultVo;

public interface UserService {
    /**

     * 错误码:
     * 1000 - 邮箱未注册
     * 1001 - 登录成功
     * 1002 - 密码错误
     * 1006 - 邮箱或密码为空
     */
    ResultVo login(String email, String password);

    /**

     * 错误码:
     * 1003 - 两次密码不一致
     * 1004 - 邮箱已存在
     * 1005 - 注册成功
     * 1007 - 所有字段不能为空
     * 1008 - 注册失败
     */
    ResultVo register(String username, String password, String confirmPassword, String email);
}