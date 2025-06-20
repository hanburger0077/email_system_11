package com.example.backend.mapper;

import com.example.backend.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param; // 用于在Mapper方法中传递多个参数

@Mapper
public interface UserMapper {
    // 查找有效状态的用户
    User findByEmail(String email);

    // 新增：查找所有状态的用户（包括已逻辑删除的），用于注册时判断邮箱是否占用
    User findAnyByEmail(String email);

    int insertUser(User user);

    // 逻辑删除时不再需要物理删除方法
    // int deleteByEmail(String email);

    // 新增：更新用户状态（用于逻辑删除）
    int updateUserStatus(@Param("email") String email, @Param("status") Integer status);

    int updatePassword(User user);

    int updateUsername(User user);
    User findById(long id);
    int updatePhone(User user);
}