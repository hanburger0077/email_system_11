package com.example.backend.service.Impl;

import com.example.backend.entity.User;
import com.example.backend.mapper.UserMapper;
import com.example.backend.service.UserService;
import com.example.backend.utils.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.regex.Pattern;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder; // 导入 BCryptPasswordEncoder

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder; // 注入 BCryptPasswordEncoder

    // 邮箱格式正则
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[0-9]{6,8}@hh\\.com$");

    private boolean isValidEmail(String email) {
        return EMAIL_PATTERN.matcher(email).matches();
    }

    @Override
    public ResultVo login(String email, String password, HttpServletResponse response) {
        if (!StringUtils.hasText(email) || !StringUtils.hasText(password)) {
            return ResultVo.fail("操作错误", "邮箱和密码不能为空");
        }
        if (!isValidEmail(email)) {
            return ResultVo.fail("操作错误", "邮箱格式错误，前6-8位数字，后缀为@hh.com");
        }

        User user = userMapper.findByEmail(email);
        if (user == null) {
            return ResultVo.fail("操作错误", "邮箱未注册");
        }

        // 验证密码：使用 BCryptPasswordEncoder.matches() 方法比对明文密码和哈希密码
        if (!bCryptPasswordEncoder.matches(password, user.getPassword())) {
            return ResultVo.fail("操作错误", "密码错误");
        }

        // 登录成功，设置cookie，模拟登录状态
        Cookie cookie = new Cookie("loginUserEmail", email);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(60 * 60 * 24); // 1天有效
        response.addCookie(cookie);

        // 登录成功时，返回的User对象不包含敏感信息，尤其是密码字段
        User loggedInUser = new User();
        loggedInUser.setId(user.getId());
        loggedInUser.setUsername(user.getUsername());
        loggedInUser.setEmail(user.getEmail());
        loggedInUser.setPhone(user.getPhone());

        return ResultVo.success("登录成功", loggedInUser);
    }

    @Override
    public ResultVo register(String username, String password, String confirmPassword, String email, String phone) {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password) ||
                !StringUtils.hasText(confirmPassword) || !StringUtils.hasText(email) || !StringUtils.hasText(phone)) {
            return ResultVo.fail("操作错误", "所有字段不能为空");
        }
        if (!isValidEmail(email)) {
            return ResultVo.fail("操作错误", "邮箱格式错误，前6-8位数字，后缀为@hh.com");
        }
        if (!password.equals(confirmPassword)) {
            return ResultVo.fail("操作错误", "两次密码不一致");
        }

        User existing = userMapper.findByEmail(email);
        if (existing != null) {
            return ResultVo.fail("操作错误", "邮箱已存在");
        }

        User newUser = new User();
        newUser.setUsername(username);
        // 对密码进行哈希处理
        newUser.setPassword(bCryptPasswordEncoder.encode(password));
        newUser.setEmail(email);
        newUser.setPhone(phone);

        int result = userMapper.insertUser(newUser);
        if (result <= 0) {
            return ResultVo.fail("操作错误", "注册失败，请重试");
        }

        // 注册成功时，返回的User对象不包含敏感信息，尤其是密码字段
        User registeredUser = new User();
        registeredUser.setId(newUser.getId());
        registeredUser.setUsername(newUser.getUsername());
        registeredUser.setEmail(newUser.getEmail());
        registeredUser.setPhone(newUser.getPhone());

        return ResultVo.success("注册成功", registeredUser);
    }

    @Override
    public ResultVo logout(String email, String password) {
        if (!StringUtils.hasText(email) || !StringUtils.hasText(password)) {
            return ResultVo.fail("操作错误", "邮箱和密码不能为空");
        }
        if (!isValidEmail(email)) {
            return ResultVo.fail("操作错误", "邮箱格式错误，前6-8位数字，后缀为@hh.com");
        }

        User user = userMapper.findByEmail(email);
        if (user == null) {
            return ResultVo.fail("操作错误", "邮箱未注册");
        }

        // 验证密码：使用 BCryptPasswordEncoder.matches() 方法比对明文密码和哈希密码
        if (!bCryptPasswordEncoder.matches(password, user.getPassword())) {
            return ResultVo.fail("操作错误", "密码错误");
        }

        // 删除用户
        int deleted = userMapper.deleteByEmail(email);
        if (deleted <= 0) {
            return ResultVo.fail("操作错误", "注销失败");
        }

        return ResultVo.success("注销成功", null);
    }

    @Override
    public ResultVo exit(HttpServletRequest request, HttpServletResponse response) {
        // 检查是否存在 loginUserEmail 这个 Cookie 并且它有值
        Cookie[] cookies = request.getCookies();
        boolean loggedIn = false;
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("loginUserEmail".equals(cookie.getName()) && StringUtils.hasText(cookie.getValue())) {
                    loggedIn = true;
                    break;
                }
            }
        }

        // 如果没有找到登录状态的 Cookie，说明当前没有用户登录
        if (!loggedIn) {
            return ResultVo.fail("操作错误", "当前没有用户处于登录状态，无法执行退出操作");
        }

        // 清除登录状态的 Cookie
        Cookie cookie = new Cookie("loginUserEmail", null);
        cookie.setPath("/");
        cookie.setMaxAge(0); // 设置有效期为0，浏览器会立即删除该 Cookie
        response.addCookie(cookie);

        return ResultVo.success("退出成功");
    }

    @Override
    public ResultVo updateUsername(String email, String password, String oldUsername, String newUsername) {
        if (!StringUtils.hasText(email) || !StringUtils.hasText(password) ||
                !StringUtils.hasText(oldUsername) || !StringUtils.hasText(newUsername)) {
            return ResultVo.fail("操作错误", "所有字段不能为空");
        }
        if (!isValidEmail(email)) {
            return ResultVo.fail("操作错误", "邮箱格式错误，前6-8位数字，后缀为@hh.com");
        }

        User user = userMapper.findByEmail(email);
        if (user == null) {
            return ResultVo.fail("操作错误", "邮箱未注册");
        }
        // 验证密码
        if (!bCryptPasswordEncoder.matches(password, user.getPassword())) {
            return ResultVo.fail("操作错误", "密码错误");
        }
        if (!user.getUsername().equals(oldUsername)) {
            return ResultVo.fail("操作错误", "旧用户名不匹配");
        }

        user.setUsername(newUsername);
        int updated = userMapper.updateUsername(user);
        if (updated <= 0) {
            return ResultVo.fail("操作错误", "用户名修改失败");
        }

        // 返回更新后的User对象（不含密码）
        User updatedUser = new User();
        updatedUser.setId(user.getId());
        updatedUser.setUsername(user.getUsername());
        updatedUser.setEmail(user.getEmail());
        updatedUser.setPhone(user.getPhone());

        return ResultVo.success("用户名修改成功", updatedUser);
    }

    @Override
    public ResultVo updatePhone(String email, String password, String oldPhone, String newPhone) {
        if (!StringUtils.hasText(email) || !StringUtils.hasText(password) ||
                !StringUtils.hasText(oldPhone) || !StringUtils.hasText(newPhone)) {
            return ResultVo.fail("操作错误", "所有字段不能为空");
        }
        if (!isValidEmail(email)) {
            return ResultVo.fail("操作错误", "邮箱格式错误，前6-8位数字，后缀为@hh.com");
        }

        User user = userMapper.findByEmail(email);
        if (user == null) {
            return ResultVo.fail("操作错误", "邮箱未注册");
        }
        // 验证密码
        if (!bCryptPasswordEncoder.matches(password, user.getPassword())) {
            return ResultVo.fail("操作错误", "密码错误");
        }
        if (!oldPhone.equals(user.getPhone())) {
            return ResultVo.fail("操作错误", "旧手机号不匹配");
        }

        user.setPhone(newPhone);
        int updated = userMapper.updatePhone(user);
        if (updated <= 0) {
            return ResultVo.fail("操作错误", "手机号修改失败");
        }

        // 返回更新后的User对象（不含密码）
        User updatedUser = new User();
        updatedUser.setId(user.getId());
        updatedUser.setUsername(user.getUsername());
        updatedUser.setEmail(user.getEmail());
        updatedUser.setPhone(user.getPhone());

        return ResultVo.success("手机号修改成功", updatedUser);
    }

    @Override
    public ResultVo updatePassword(String email, String oldPassword, String newPassword) {
        if (!StringUtils.hasText(email) || !StringUtils.hasText(oldPassword) || !StringUtils.hasText(newPassword)) {
            return ResultVo.fail("操作错误", "所有字段不能为空");
        }
        if (!isValidEmail(email)) {
            return ResultVo.fail("操作错误", "邮箱格式错误，前6-8位数字，后缀为@hh.com");
        }

        User user = userMapper.findByEmail(email);
        if (user == null) {
            return ResultVo.fail("操作错误", "邮箱未注册");
        }
        // 验证旧密码
        if (!bCryptPasswordEncoder.matches(oldPassword, user.getPassword())) {
            return ResultVo.fail("操作错误", "旧密码不正确"); // 密码错误提示更具体
        }

        // 对新密码进行哈希处理
        user.setPassword(bCryptPasswordEncoder.encode(newPassword));
        int updated = userMapper.updatePassword(user);
        if (updated <= 0) {
            return ResultVo.fail("操作错误", "密码修改失败");
        }

        // 返回更新后的User对象（不含密码）
        User updatedUser = new User();
        updatedUser.setId(user.getId());
        updatedUser.setUsername(user.getUsername());
        updatedUser.setEmail(user.getEmail());
        updatedUser.setPhone(user.getPhone());

        return ResultVo.success("密码修改成功", updatedUser);
    }
}

