package com.example.backend.service;
import com.example.backend.entity.User;
import com.example.backend.mapper.UserMapper;
import com.example.backend.utils.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequest; // 新增导入
import java.util.regex.Pattern;

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserMapper userMapper;
    // 邮箱格式正则
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[0-9]{6,8}@hh\\.com$");
    private boolean isValidEmail(String email) {
        return EMAIL_PATTERN.matcher(email).matches();
    }
    @Override
    public ResultVo login(String email, String password, HttpServletResponse response) {
        if (!StringUtils.hasText(email) || !StringUtils.hasText(password)) {
            return new ResultVo(1006, "邮箱和密码不能为空", null);
        }
        if (!isValidEmail(email)) {
            return new ResultVo(1009, "邮箱格式错误，前6-8位数字，后缀为@hh.com", null);
        }
        User user = userMapper.findByEmail(email);
        if (user == null) {
            return new ResultVo(1000, "邮箱未注册", null);
        }
        if (!user.getPassword().equals(password)) {
            return new ResultVo(1002, "密码错误", null);
        }
        // 登录成功，设置cookie，模拟登录状态
        Cookie cookie = new Cookie("loginUserEmail", email);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(60 * 60 * 24); // 1天有效
        response.addCookie(cookie);

        return new ResultVo(1001, "登录成功", user);
    }
    @Override
    public ResultVo register(String username, String password, String confirmPassword, String email, String phone) {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password) ||
                !StringUtils.hasText(confirmPassword) || !StringUtils.hasText(email) || !StringUtils.hasText(phone)) {
            return new ResultVo(1007, "所有字段不能为空", null);
        }
        if (!isValidEmail(email)) {
            return new ResultVo(1009, "邮箱格式错误，前6-8位数字，后缀为@hh.com", null);
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
        newUser.setPhone(phone);
        int result = userMapper.insertUser(newUser);
        if (result <= 0) {
            return new ResultVo(1008, "注册失败，请重试", null);
        }

        return new ResultVo(1005, "注册成功", newUser);
    }
    @Override
    public ResultVo logout(String email, String password) {
        if (!StringUtils.hasText(email) || !StringUtils.hasText(password)) {
            return new ResultVo(1006, "邮箱和密码不能为空", null);
        }
        if (!isValidEmail(email)) {
            return new ResultVo(1009, "邮箱格式错误，前6-8位数字，后缀为@hh.com", null);
        }
        User user = userMapper.findByEmail(email);
        if (user == null) {
            return new ResultVo(1000, "邮箱未注册", null);
        }
        if (!user.getPassword().equals(password)) {
            return new ResultVo(1002, "密码错误", null);
        }
        // 删除用户
        int deleted = userMapper.deleteByEmail(email);
        if (deleted <= 0) {
            return new ResultVo(1010, "注销失败", null);
        }
        return new ResultVo(1011, "注销成功", null);
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
            return ResultVo.fail(1021, "当前没有用户处于登录状态，无法执行退出操作"); // 使用新的错误码
        }
        // 清除登录状态的 Cookie
        Cookie cookie = new Cookie("loginUserEmail", null);
        cookie.setPath("/");
        cookie.setMaxAge(0); // 设置有效期为0，浏览器会立即删除该 Cookie
        response.addCookie(cookie);
        return ResultVo.success("退出成功"); // 使用 ResultVo.success 静态方法
    }
    @Override
    public ResultVo updateUsername(String email, String password, String oldUsername, String newUsername) {
        if (!StringUtils.hasText(email) || !StringUtils.hasText(password) ||
                !StringUtils.hasText(oldUsername) || !StringUtils.hasText(newUsername)) {
            return new ResultVo(1007, "所有字段不能为空", null);
        }
        if (!isValidEmail(email)) {
            return new ResultVo(1009, "邮箱格式错误，前6-8位数字，后缀为@hh.com", null);
        }

        User user = userMapper.findByEmail(email);
        if (user == null) {
            return new ResultVo(1000, "邮箱未注册", null);
        }
        if (!user.getPassword().equals(password)) {
            return new ResultVo(1002, "密码错误", null);
        }
        if (!user.getUsername().equals(oldUsername)) {
            return new ResultVo(1013, "旧用户名不匹配", null);
        }
        user.setUsername(newUsername);
        int updated = userMapper.updateUsername(user);
        if (updated <= 0) {
            return new ResultVo(1014, "用户名修改失败", null);
        }
        return new ResultVo(1015, "用户名修改成功", user);
    }
    @Override
    public ResultVo updatePhone(String email, String password, String oldPhone, String newPhone) {
        if (!StringUtils.hasText(email) || !StringUtils.hasText(password) ||
                !StringUtils.hasText(oldPhone) || !StringUtils.hasText(newPhone)) {
            return new ResultVo(1007, "所有字段不能为空", null);
        }
        if (!isValidEmail(email)) {
            return new ResultVo(1009, "邮箱格式错误，前6-8位数字，后缀为@hh.com", null);
        }
        User user = userMapper.findByEmail(email);
        if (user == null) {
            return new ResultVo(1000, "邮箱未注册", null);
        }
        if (!user.getPassword().equals(password)) {
            return new ResultVo(1002, "密码错误", null);
        }
        if (!oldPhone.equals(user.getPhone())) {
            return new ResultVo(1016, "旧手机号不匹配", null);
        }
        user.setPhone(newPhone);
        int updated = userMapper.updatePhone(user);
        if (updated <= 0) {
            return new ResultVo(1017, "手机号修改失败", null);
        }
        return new ResultVo(1018, "手机号修改成功", user);
    }
    @Override
    public ResultVo updatePassword(String email, String oldPassword, String newPassword) {
        if (!StringUtils.hasText(email) || !StringUtils.hasText(oldPassword) || !StringUtils.hasText(newPassword)) {
            return new ResultVo(1007, "所有字段不能为空", null);
        }
        if (!isValidEmail(email)) {
            return new ResultVo(1009, "邮箱格式错误，前6-8位数字，后缀为@hh.com", null);
        }
        User user = userMapper.findByEmail(email);
        if (user == null) {
            return new ResultVo(1000, "邮箱未注册", null);
        }
        if (!user.getPassword().equals(oldPassword)) {
            return new ResultVo(1002, "密码错误", null);
        }
        user.setPassword(newPassword);
        int updated = userMapper.updatePassword(user);
        if (updated <= 0) {
            return new ResultVo(1019, "密码修改失败", null);
        }
        return new ResultVo(1020, "密码修改成功", user);
    }
}