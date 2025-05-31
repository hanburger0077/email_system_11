package com.example.backend.service;

import com.example.backend.dao.UserDao;
import com.example.backend.entity.User;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
public class UserService {

    private final SqlSessionFactory sqlSessionFactory;
    private final BCryptPasswordEncoder passwordEncoder;

    @Autowired
    public UserService(SqlSessionFactory sqlSessionFactory,
                       BCryptPasswordEncoder passwordEncoder) {
        this.sqlSessionFactory = sqlSessionFactory;
        this.passwordEncoder = passwordEncoder;
    }

    private UserDao getUserDao() {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            return session.getMapper(UserDao.class);
        }
    }

    public String register(User user) {
        UserDao userDao = getUserDao();

        // 检查用户名是否存在
        if (userDao.findByUsername(user.getUsername()) != null) {
            return "用户名已存在";
        }

        // 检查邮箱是否存在
        if (userDao.findByEmail(user.getEmailAddress()) != null) {
            return "邮箱已被注册";
        }

        // 设置时间戳
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());

        // 密码加密
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // 插入用户
        int result = userDao.insertUser(user);
        return result > 0 ? "注册成功" : "注册失败";
    }

    public String login(String username, String password) {
        UserDao userDao = getUserDao();
        User user = userDao.findByUsername(username);

        if (user == null) {
            return "用户不存在";
        }

        // 验证密码
        if (passwordEncoder.matches(password, user.getPassword())) {
            return "登录成功";
        } else {
            return "密码错误";
        }
    }
}