package com.example.backend.controller;

import com.example.backend.entity.Mail;
import com.example.backend.service.MailService;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import com.example.backend.service.MailService;

import java.time.LocalDateTime;

@RestController
public class MailController {

    @Autowired
    private MailService mailService;

    @GetMapping("/send-mail")
    public String sendMail() throws MessagingException {

        // 创建并插入一个测试邮件
        Mail mail = new Mail();
        mail.setSender_id(1L);  // 确保 senderId 在 user 表中存在
        mail.setReceiver_id(2L); // 确保 receiverId 在 user 表中存在
        mail.setSubject("Test Subject");
        mail.setContent("Test Content");
        mail.setCreate_at(LocalDateTime.now());
        mailService.sendMail("2", "Test Subject","Test Content");
        return "Email sent successfully!";
    }
}
