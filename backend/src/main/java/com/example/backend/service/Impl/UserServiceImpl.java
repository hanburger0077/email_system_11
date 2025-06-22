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
import java.security.SecureRandom;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    // 邮箱格式正则：后缀为 @flowmail.com，前6-8位数字
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[0-9]{6,8}@flowmail\\.com$");

    // 密码格式正则：至少包含大小写字母和数字，长度6-20位
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[a-zA-Z\\d]{6,20}$");

    // **新增：手机号码格式正则：精确11位数字**
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\d{11}$");

    private boolean isValidEmail(String email) {
        return EMAIL_PATTERN.matcher(email).matches();
    }

    private boolean isValidPassword(String password) {
        return PASSWORD_PATTERN.matcher(password).matches();
    }

    // **新增：手机号码格式校验方法**
    private boolean isValidPhone(String phone) {
        return PHONE_PATTERN.matcher(phone).matches();
    }

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

    // --- 工具方法：生成8位随机字符的恢复码 ---
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int RECOVERY_CODE_LENGTH = 8; // 恢复码长度
    private SecureRandom secureRandom = new SecureRandom(); // 使用 SecureRandom

    private String generateSecureRecoveryCode() {
        StringBuilder sb = new StringBuilder(RECOVERY_CODE_LENGTH);
        for (int i = 0; i < RECOVERY_CODE_LENGTH; i++) {
            // 从 CHARACTERS 字符串中随机选择一个字符
            int randomIndex = secureRandom.nextInt(CHARACTERS.length());
            sb.append(CHARACTERS.charAt(randomIndex));
        }
        return sb.toString();
    }
    // --- 工具方法结束 ---

    @Override
    public ResultVo login(String email, String password, HttpServletResponse response) {
        if (!StringUtils.hasText(email) || !StringUtils.hasText(password)) {
            return ResultVo.fail("操作错误", "邮箱和密码不能为空", "邮箱或密码字段为空");
        }
        if (!isValidEmail(email)) {
            return ResultVo.fail("操作错误", "邮箱格式错误", "邮箱格式错误，前6-8位数字，后缀为@flowmail.com");
        }

        User user = userMapper.findByEmail(email);
        if (user == null) {
            return ResultVo.fail("操作错误", "邮箱未注册或密码错误", "用户不存在或密码不匹配");
        }

        if (!bCryptPasswordEncoder.matches(password, user.getPassword())) {
            return ResultVo.fail("操作错误", "邮箱未注册或密码错误", "用户不存在或密码不匹配");
        }

        Cookie cookie = new Cookie("loginUserEmail", email);
        cookie.setPath("/");
        cookie.setHttpOnly(false);  // 允许JavaScript访问
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
            return ResultVo.fail("操作错误", "所有字段不能为空", "注册信息不完整");
        }
        if (!isValidEmail(email)) {
            return ResultVo.fail("操作错误", "邮箱格式错误", "邮箱格式错误，前6-8位数字，后缀为@flowmail.com");
        }
        if (!password.equals(confirmPassword)) {
            return ResultVo.fail("操作错误", "两次密码不一致", "确认密码与输入密码不匹配");
        }
        if (!isValidPassword(password)) {
            return ResultVo.fail("操作错误", "密码格式不符合要求", "密码必须包含大小写字母和数字，长度6-20位");
        }
        // **新增：注册时手机号码格式校验**
        if (!isValidPhone(phone)) {
            return ResultVo.fail("操作错误", "手机号码格式错误", "手机号码必须是11位数字");
        }

        User existing = userMapper.findAnyByEmail(email);
        if (existing != null) {
            return ResultVo.fail("操作错误", "邮箱已存在", "该邮箱已被注册");
        }

        String plainRecoveryCode = generateSecureRecoveryCode(); // 生成8位随机明文恢复码
        String hashedRecoveryCode = bCryptPasswordEncoder.encode(plainRecoveryCode); // 对明文恢复码进行哈希

        User newUser = new User();
        newUser.setUsername(username);
        newUser.setPassword(bCryptPasswordEncoder.encode(password));
        newUser.setEmail(email);
        newUser.setPhone(phone);
        newUser.setStatus(1); // 新用户默认为有效状态
        newUser.setHashedRecoveryCode(hashedRecoveryCode); // 存储哈希后的恢复码到数据库

        int result = userMapper.insertUser(newUser);
        if (result <= 0) {
            return ResultVo.fail("操作错误", "注册失败，请重试", "数据库插入失败");
        }

        User registeredUser = new User();
        registeredUser.setId(newUser.getId());
        registeredUser.setUsername(newUser.getUsername());
        registeredUser.setEmail(newUser.getEmail());
        registeredUser.setPhone(newUser.getPhone());
        registeredUser.setStatus(newUser.getStatus());
        return ResultVo.success("注册成功！请妥善保管您的恢复码： " + plainRecoveryCode + "。此码仅显示一次。", registeredUser);
    }

    @Override
    public ResultVo logout(HttpServletRequest request, String password) {
        String loggedInEmail = getLoggedInUserEmail(request);

        if (!StringUtils.hasText(loggedInEmail)) {
            return ResultVo.fail("操作错误", "用户未登录", "无法执行注销操作，因为用户未登录");
        }

        if (!StringUtils.hasText(password)) {
            return ResultVo.fail("操作错误", "请提供密码进行注销确认", "注销操作需要密码确认");
        }

        User user = userMapper.findByEmail(loggedInEmail);
        if (user == null) {
            return ResultVo.fail("操作错误", "用户不存在或已是注销状态", "登录会话有效但用户在数据库中不存在或已失效");
        }

        if (!bCryptPasswordEncoder.matches(password, user.getPassword())) {
            return ResultVo.fail("操作错误", "密码不正确", "提供的密码与当前用户密码不匹配，无法注销");
        }

        int updated = userMapper.updateUserStatus(loggedInEmail, 0);
        if (updated <= 0) {
            return ResultVo.fail("操作错误", "注销失败，请重试", "数据库更新用户状态失败");
        }

        return ResultVo.success("注销成功", null);
    }

    @Override
    public ResultVo exit(HttpServletRequest request, HttpServletResponse response) {
        String loggedInEmail = getLoggedInUserEmail(request);
        if (!StringUtils.hasText(loggedInEmail)) {
            return ResultVo.fail("操作错误", "当前没有用户处于登录状态", "无法执行退出操作，因为用户未登录");
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
            return ResultVo.fail("操作错误", "用户未登录", "无法修改用户名，请先登录");
        }
        if (!StringUtils.hasText(newUsername)) {
            return ResultVo.fail("操作错误", "新用户名不能为空", "请输入新的用户名");
        }

        User user = userMapper.findByEmail(email);
        if (user == null) {
            return ResultVo.fail("操作错误", "用户不存在或已注销", "当前登录用户无效");
        }
        if (user.getUsername().equals(newUsername)) {
            return ResultVo.fail("操作错误", "新用户名与旧用户名相同", "请提供不同的用户名");
        }

        user.setUsername(newUsername);
        int updated = userMapper.updateUsername(user);
        if (updated <= 0) {
            return ResultVo.fail("操作错误", "用户名修改失败", "数据库更新用户名失败");
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
            return ResultVo.fail("操作错误", "用户未登录", "无法修改手机号，请先登录");
        }
        if (!StringUtils.hasText(newPhone)) {
            return ResultVo.fail("操作错误", "新手机号不能为空", "请输入新的手机号码");
        }
        // **新增：修改手机号时格式校验**
        if (!isValidPhone(newPhone)) {
            return ResultVo.fail("操作错误", "新手机号码格式错误", "新手机号码必须是11位数字");
        }

        User user = userMapper.findByEmail(email);
        if (user == null) {
            return ResultVo.fail("操作错误", "用户不存在或已注销", "当前登录用户无效");
        }
        if (user.getPhone() != null && user.getPhone().equals(newPhone)) {
            return ResultVo.fail("操作错误", "新手机号与旧手机号相同", "请提供不同的手机号码");
        }

        user.setPhone(newPhone);
        int updated = userMapper.updatePhone(user);
        if (updated <= 0) {
            return ResultVo.fail("操作错误", "手机号修改失败", "数据库更新手机号失败");
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
            return ResultVo.fail("操作错误", "用户未登录", "无法修改密码，请先登录");
        }
        if (!StringUtils.hasText(oldPassword) || !StringUtils.hasText(newPassword) || !StringUtils.hasText(confirmNewPassword)) {
            return ResultVo.fail("操作错误", "所有密码字段不能为空", "旧密码、新密码和确认密码都必须填写");
        }
        if (!newPassword.equals(confirmNewPassword)) {
            return ResultVo.fail("操作错误", "两次新密码不一致", "新密码和确认新密码不匹配");
        }
        if (!isValidPassword(newPassword)) {
            return ResultVo.fail("操作错误", "新密码格式不符合要求", "新密码必须包含大小写字母和数字，长度6-20位");
        }

        User user = userMapper.findByEmail(email);
        if (user == null) {
            return ResultVo.fail("操作错误", "用户不存在或已注销", "当前登录用户无效");
        }
        if (!bCryptPasswordEncoder.matches(oldPassword, user.getPassword())) {
            return ResultVo.fail("操作错误", "旧密码不正确", "输入的旧密码与当前密码不匹配");
        }
        if (bCryptPasswordEncoder.matches(newPassword, user.getPassword())) {
            return ResultVo.fail("操作错误", "新密码不能与旧密码相同", "请设置一个与旧密码不同的新密码");
        }

        user.setPassword(bCryptPasswordEncoder.encode(newPassword));
        int updated = userMapper.updatePassword(user);
        if (updated <= 0) {
            return ResultVo.fail("操作错误", "密码修改失败", "数据库更新密码失败");
        }

        User updatedUser = new User();
        updatedUser.setId(user.getId());
        updatedUser.setUsername(user.getUsername());
        updatedUser.setEmail(user.getEmail());
        updatedUser.setPhone(user.getPhone());
        updatedUser.setStatus(user.getStatus());

        return ResultVo.success("密码修改成功", updatedUser);
    }

    @Override
    public ResultVo recoverPassword(String email, String recoveryCode, String newPassword, String confirmNewPassword) {
        if (!StringUtils.hasText(email) || !StringUtils.hasText(recoveryCode) ||
                !StringUtils.hasText(newPassword) || !StringUtils.hasText(confirmNewPassword)) {
            return ResultVo.fail("操作错误", "邮箱、恢复码和新密码都不能为空", "请填写所有必填字段");
        }
        if (!isValidEmail(email)) {
            return ResultVo.fail("操作错误", "邮箱格式错误", "邮箱格式错误，前6-8位数字，后缀为@flowmail.com");
        }
        if (!newPassword.equals(confirmNewPassword)) {
            return ResultVo.fail("操作错误", "两次新密码不一致", "新密码和确认新密码不匹配");
        }
        if (!isValidPassword(newPassword)) {
            return ResultVo.fail("操作错误", "新密码格式不符合要求", "新密码必须包含大小写字母和数字，长度6-20位");
        }

        User user = userMapper.findAnyByEmail(email);
        if (user == null) {
            return ResultVo.fail("操作错误", "用户不存在", "指定邮箱未注册");
        }

        if (user.getHashedRecoveryCode() == null || !bCryptPasswordEncoder.matches(recoveryCode, user.getHashedRecoveryCode())) {
            return ResultVo.fail("操作错误", "恢复码不正确", "提供的恢复码与系统记录不匹配");
        }

        user.setPassword(bCryptPasswordEncoder.encode(newPassword));

        String newPlainRecoveryCode = generateSecureRecoveryCode();
        String newHashedRecoveryCode = bCryptPasswordEncoder.encode(newPlainRecoveryCode);
        user.setHashedRecoveryCode(newHashedRecoveryCode);

        if (user.getStatus() == 0) {
            user.setStatus(1); // 如果是注销用户，恢复密码后将其状态改回正常
        }

        int updatedPassword = userMapper.updatePassword(user);
        int updatedRecoveryCode = userMapper.updateRecoveryCode(user);

        if (updatedPassword <= 0 || updatedRecoveryCode <= 0) {
            return ResultVo.fail("操作错误", "密码重置失败，请重试", "数据库更新操作失败");
        }

        return ResultVo.success("密码重置成功！请妥善保管您的新恢复码：" + newPlainRecoveryCode + "。此码仅显示一次。", null);
    }
}