package com.example.backend.dao;

import com.example.backend.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserDao {
    User findByUsername(@Param("username") String username);
    User findByEmail(@Param("email") String email);
    int insertUser(User user);
}