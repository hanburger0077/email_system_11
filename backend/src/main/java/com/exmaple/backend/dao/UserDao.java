package com.exmaple.backend.dao;

import com.exmaple.backend.entity.User;
import org.apache.ibatis.annotations.Param;

// 移除 @Mapper 注解
public interface UserDao {
    User findByUsername(@Param("username") String username);
    User findByEmail(@Param("email") String email);
    int insertUser(User user);
}