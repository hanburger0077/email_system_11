package com.example.backend.service;

import com.example.backend.entity.User;
import com.example.backend.mapper.UserMapper;
import com.example.backend.utils.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Override
    public ResultVo login(String email, String password) {
        // 参数校验
        if (!StringUtils.hasText(email) || !StringUtils.hasText(password)) {
            return new ResultVo(1006, "邮箱和密码不能为空", null);
        }

        User user = userMapper.findByEmail(email);
        if (user == null) {
            return new ResultVo(1000, "邮箱未注册", null);
        }

        if (!user.getPassword().equals(password)) {
            return new ResultVo(1002, "密码错误", null);
        }

        return new ResultVo(1001, "登录成功", user);
    }

    @Override
    public ResultVo register(String username, String password, String confirmPassword, String email) {
        // 参数校验
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password) ||
                !StringUtils.hasText(confirmPassword) || !StringUtils.hasText(email)) {
            return new ResultVo(1007, "所有字段不能为空", null);
        }

        if (!password.equals(confirmPassword)) {
            return new ResultVo(1003, "两次密码不一致", null);
        }

        User existing = userMapper.findByEmail(email);
        if (existing != null) {
            return new ResultVo(1004, "邮箱已存在", null);
        }

        User newUser = new User();
        newUser.setUsername(username);
        newUser.setPassword(password);
        newUser.setEmail(email);

        int result = userMapper.insertUser(newUser);
        if (result <= 0) {
            return new ResultVo(1008, "注册失败，请重试", null);
        }

        return new ResultVo(1005, "注册成功", newUser);
    }
}