package com.example.backend.mapper;

import com.example.backend.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper {
    User findByEmail(String email);
    int insertUser(User user);  //新增注册功能
    int deleteByEmail(String email);//新增注销功能
    int updatePassword(User user);//新增修改密码功能
    int updateUsername(User user);//新增修改用户名功能
    int updatePhone(User user);//新增修改电话号码功能
    User findById(long id);
    boolean authenticate(String email, String password);
}