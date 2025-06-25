// com.example.backend.mapper.UserMapper.java
package com.example.backend.mapper;

import com.example.backend.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param; // 需要导入 @Param 注解，虽然这里主要用User对象传递，但良好的习惯是导入

@Mapper
public interface UserMapper {
    // 现有方法保持不变
    User findByEmail(@Param("email") String email);
    User findAnyByEmail(@Param("email") String email);
    int insertUser(User user);
    int updateUserStatus(@Param("email") String email, @Param("status") int status);
    int updatePassword(User user);
    int updateUsername(User user);
    int updatePhone(User user);
    int updateRecoveryCode(User user);
    User findById(@Param("id") Long id);

    /**
     * 用于密码恢复：更新用户密码、恢复码，并强制设置状态为激活 (status = 1)。
     * 此方法适用于用户无论是否为已注销状态时进行密码重置。
     * @param user 包含新密码、新哈希恢复码和用户邮箱的User对象。
     * @return 更新的行数。
     */
    int updatePasswordAndRecoveryCodeAndActivate(User user);
}