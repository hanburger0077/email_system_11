package com.example.backend.controller;

import com.example.backend.Client.SmtpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MailController {

    //发送方
    @Value("${mail.username}")
    private String from;

    private final SmtpClient smtpClient;

    @Autowired
    public MailController(SmtpClient smtpClient) {
        this.smtpClient = smtpClient;
    }

    @RequestMapping("/send-mail")
    public String sendMail(
            @RequestParam String to,
            @RequestParam String subject,
            @RequestParam String content,
            @RequestParam(required = false) String attachmentName,
            @RequestParam(required = false) byte[] attachmentContent) {
        try {
            this.smtpClient.connect();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to connect to SMTP server", e);
        }
        try {
            if (attachmentName != null && attachmentContent != null) {
                // 发送带有附件的邮件
                smtpClient.sendMail(
                        from, // 发件人地址
                        to,                   // 收件人地址
                        subject,              // 邮件主题
                        content,              // 邮件正文（纯文本部分）
                        "<h1>" + content + "</h1>", // 邮件正文（HTML部分）
                        attachmentName,       // 附件名称
                        attachmentContent     // 附件内容
                );
            } else {
                // 发送不带附件的邮件
                smtpClient.sendSimpleMail(
                        from, // 发件人地址
                        to,                   // 收件人地址
                        subject,              // 邮件主题
                        content               // 邮件正文
                );
            }
            return "Email sent successfully!";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "Failed to send email: " + e.getMessage();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            // 关闭连接
            smtpClient.disconnect();
        }
    }
}
