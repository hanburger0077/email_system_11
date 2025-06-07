package com.example.backend.service.Impl;


import com.example.backend.Client.ImapClient;
import com.example.backend.Client.SmtpClient;
import com.example.backend.DTO.MailDTO;
import com.example.backend.service.MailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


@Service
public class MailServiceImpl implements MailService {

    // 用户邮箱
    @Value("${mail.username}")
    private String userEmail;

    // 用户密码
    @Value("${mail.password}")
    private String userPassword;

    @Autowired
    private final SmtpClient smtpClient;

    @Autowired
    private final ImapClient imapClient;

    public MailServiceImpl(SmtpClient smtpClient, ImapClient imapClient) {
        this.smtpClient = smtpClient;
        this.imapClient = imapClient;
    }

    public String sendMail(String to, String subject, String content, String attachmentName, byte[] attachmentContent){
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
                        userEmail, // 发件人地址
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
                        userEmail, // 发件人地址
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

    public String fetchMail(long mailId) {
        try {
            this.imapClient.connect();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to connect to IMAP server", e);
        }
        try {
            imapClient.loginCommand(userEmail, userPassword);
        } catch (InterruptedException e) {
            throw new RuntimeException("Failed to send Login command", e);
        }
        MailDTO mailDTO;
        try {
            mailDTO = imapClient.fetchCommand(mailId);
        } catch (InterruptedException e) {
            throw new RuntimeException("Failed to send FETCH command",e);
        }
        String mailStr = "发件人：\t" + mailDTO.getSender_email() + "<br>"
                        + "收件人：\t" + mailDTO.getReceiver_email() + "<br>"
                        + "主题：\t" + mailDTO.getSubject() + "<br>"
                        + "内容：\t" + mailDTO.getContent() + "<br>"
                        + "时间：\t" + mailDTO.getCreate_at().toString();
        return mailStr.replace("\n","<br>");
    }
}
