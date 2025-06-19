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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;


@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    // 邮箱格式正则：后缀为 @flowmail.com
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[0-9]{6,8}@flowmail\\.com$");

    // 密码格式正则：至少包含大小写字母和数字，长度6-20位
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[a-zA-Z\\d]{6,20}$");


    private boolean isValidEmail(String email) {
        return EMAIL_PATTERN.matcher(email).matches();
    }

    private boolean isValidPassword(String password) {
        return PASSWORD_PATTERN.matcher(password).matches();
    }

    // --- 工具方法：从请求中获取登录邮箱 ---
    private String getLoggedInUserEmail(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("loginUserEmail".equals(cookie.getName()) && StringUtils.hasText(cookie.getValue())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
    // ------------------------------------

    @Override
    public ResultVo login(String email, String password, HttpServletResponse response) {
        if (!StringUtils.hasText(email) || !StringUtils.hasText(password)) {
            return ResultVo.fail("操作错误", "邮箱和密码不能为空");
        }
        if (!isValidEmail(email)) {
            return ResultVo.fail("操作错误", "邮箱格式错误，前6-8位数字，后缀为@flowmail.com");
        }

        User user = userMapper.findByEmail(email);
        if (user == null) {
            return ResultVo.fail("操作错误", "邮箱未注册或密码错误");
        }

        if (!bCryptPasswordEncoder.matches(password, user.getPassword())) {
            return ResultVo.fail("操作错误", "邮箱未注册或密码错误");
        }

        Cookie cookie = new Cookie("loginUserEmail", email);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(60 * 60 * 24); // 1天有效
        response.addCookie(cookie);

        User loggedInUser = new User();
        loggedInUser.setId(user.getId());
        loggedInUser.setUsername(user.getUsername());
        loggedInUser.setEmail(user.getEmail());
        loggedInUser.setPhone(user.getPhone());
        loggedInUser.setStatus(user.getStatus());

        return ResultVo.success("登录成功", loggedInUser);
    }

    @Override
    public ResultVo register(String username, String password, String confirmPassword, String email, String phone) {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password) ||
                !StringUtils.hasText(confirmPassword) || !StringUtils.hasText(email) || !StringUtils.hasText(phone)) {
            return ResultVo.fail("操作错误", "所有字段不能为空");
        }
        if (!isValidEmail(email)) {
            return ResultVo.fail("操作错误", "邮箱格式错误，前6-8位数字，后缀为@flowmail.com");
        }
        if (!password.equals(confirmPassword)) {
            return ResultVo.fail("操作错误", "两次密码不一致");
        }
        if (!isValidPassword(password)) {
            return ResultVo.fail("操作错误", "密码必须包含大小写字母和数字，长度6-20位");
        }

        User existing = userMapper.findAnyByEmail(email);
        if (existing != null) {
            return ResultVo.fail("操作错误", "邮箱已存在");
        }

        User newUser = new User();
        newUser.setUsername(username);
        newUser.setPassword(bCryptPasswordEncoder.encode(password));
        newUser.setEmail(email);
        newUser.setPhone(phone);
        newUser.setStatus(1); // 新用户默认为有效状态

        int result = userMapper.insertUser(newUser);
        if (result <= 0) {
            return ResultVo.fail("操作错误", "注册失败，请重试");
        }

        User registeredUser = new User();
        registeredUser.setId(newUser.getId());
        registeredUser.setUsername(newUser.getUsername());
        registeredUser.setEmail(newUser.getEmail());
        registeredUser.setPhone(newUser.getPhone());
        registeredUser.setStatus(newUser.getStatus());

        return ResultVo.success("注册成功", registeredUser);
    }

    @Override
    public ResultVo logout(HttpServletRequest request, String password) { // 增加 password 参数
        String loggedInEmail = getLoggedInUserEmail(request);

        if (!StringUtils.hasText(loggedInEmail)) {
            return ResultVo.fail("操作错误", "用户未登录，无法执行注销操作");
        }
        if (!StringUtils.hasText(password)) { // 检查密码是否为空
            return ResultVo.fail("操作错误", "请提供密码以确认注销");
        }

        User user = userMapper.findByEmail(loggedInEmail);
        if (user == null) {
            return ResultVo.fail("操作错误", "用户不存在或已是注销状态");
        }

        // 验证用户输入的密码是否正确
        if (!bCryptPasswordEncoder.matches(password, user.getPassword())) {
            return ResultVo.fail("操作错误", "密码不正确，无法注销");
        }

        // 执行逻辑删除：更新当前登录用户的状态为0
        int updated = userMapper.updateUserStatus(loggedInEmail, 0);
        if (updated <= 0) {
            return ResultVo.fail("操作错误", "注销失败，请重试");
        }

        return ResultVo.success("注销成功", null);
    }

    @Override
    public ResultVo exit(HttpServletRequest request, HttpServletResponse response) {
        String loggedInEmail = getLoggedInUserEmail(request);
        if (!StringUtils.hasText(loggedInEmail)) {
            return ResultVo.fail("操作错误", "当前没有用户处于登录状态，无法执行退出操作");
        }

        Cookie cookie = new Cookie("loginUserEmail", null);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);

        return ResultVo.success("退出成功");
    }

    @Override
    public ResultVo updateUsername(HttpServletRequest request, String newUsername) {
        String email = getLoggedInUserEmail(request);

        if (!StringUtils.hasText(email)) {
            return ResultVo.fail("操作错误", "用户未登录，无法修改用户名");
        }
        if (!StringUtils.hasText(newUsername)) {
            return ResultVo.fail("操作错误", "新用户名不能为空");
        }

        User user = userMapper.findByEmail(email);
        if (user == null) {
            return ResultVo.fail("操作错误", "用户不存在或已注销");
        }
        if (user.getUsername().equals(newUsername)) {
            return ResultVo.fail("操作错误", "新用户名与旧用户名相同");
        }

        user.setUsername(newUsername);
        int updated = userMapper.updateUsername(user);
        if (updated <= 0) {
            return ResultVo.fail("操作错误", "用户名修改失败");
        }

        User updatedUser = new User();
        updatedUser.setId(user.getId());
        updatedUser.setUsername(user.getUsername());
        updatedUser.setEmail(user.getEmail());
        updatedUser.setPhone(user.getPhone());
        updatedUser.setStatus(user.getStatus());

        return ResultVo.success("用户名修改成功", updatedUser);
    }

    @Override
    public ResultVo updatePhone(HttpServletRequest request, String newPhone) {
        String email = getLoggedInUserEmail(request);

        if (!StringUtils.hasText(email)) {
            return ResultVo.fail("操作错误", "用户未登录，无法修改手机号");
        }
        if (!StringUtils.hasText(newPhone)) {
            return ResultVo.fail("操作错误", "新手机号不能为空");
        }

        User user = userMapper.findByEmail(email);
        if (user == null) {
            return ResultVo.fail("操作错误", "用户不存在或已注销");
        }
        if (user.getPhone().equals(newPhone)) {
            return ResultVo.fail("操作错误", "新手机号与旧手机号相同");
        }

        user.setPhone(newPhone);
        int updated = userMapper.updatePhone(user);
        if (updated <= 0) {
            return ResultVo.fail("操作错误", "手机号修改失败");
        }

        User updatedUser = new User();
        updatedUser.setId(user.getId());
        updatedUser.setUsername(user.getUsername());
        updatedUser.setEmail(user.getEmail());
        updatedUser.setPhone(user.getPhone());
        updatedUser.setStatus(user.getStatus());

        return ResultVo.success("手机号修改成功", updatedUser);
    }

    @Override
    public ResultVo updatePassword(HttpServletRequest request, String oldPassword, String newPassword, String confirmNewPassword) {
        String email = getLoggedInUserEmail(request);

        if (!StringUtils.hasText(email)) {
            return ResultVo.fail("操作错误", "用户未登录，无法修改密码");
        }

        if (!StringUtils.hasText(oldPassword) ||
                !StringUtils.hasText(newPassword) || !StringUtils.hasText(confirmNewPassword)) {
            return ResultVo.fail("操作错误", "所有密码字段不能为空");
        }
        if (!newPassword.equals(confirmNewPassword)) {
            return ResultVo.fail("操作错误", "两次新密码不一致");
        }
        if (!isValidPassword(newPassword)) {
            return ResultVo.fail("操作错误", "新密码必须包含大小写字母和数字，长度6-20位");
        }

        User user = userMapper.findByEmail(email);
        if (user == null) {
            return ResultVo.fail("操作错误", "用户不存在或已注销");
        }
        if (!bCryptPasswordEncoder.matches(oldPassword, user.getPassword())) {
            return ResultVo.fail("操作错误", "旧密码不正确");
        }
        if (bCryptPasswordEncoder.matches(newPassword, user.getPassword())) {
            return ResultVo.fail("操作错误", "新密码不能与旧密码相同");
        }

        user.setPassword(bCryptPasswordEncoder.encode(newPassword));
        int updated = userMapper.updatePassword(user);
        if (updated <= 0) {
            return ResultVo.fail("操作错误", "密码修改失败");
        }

        User updatedUser = new User();
        updatedUser.setId(user.getId());
        updatedUser.setUsername(user.getUsername());
        updatedUser.setEmail(user.getEmail());
        updatedUser.setPhone(user.getPhone());
        updatedUser.setStatus(user.getStatus());

        return ResultVo.success("密码修改成功", updatedUser);
    }
}
