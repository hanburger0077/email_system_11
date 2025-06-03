package com.example.backend.service.impl;

import com.example.backend.entity.User;
import com.example.backend.mapper.UserMapper;
import com.example.backend.service.MailService;
import com.icegreen.greenmail.util.GreenMail;
import jakarta.mail.*;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import com.example.backend.mapper.MailMapper;
import com.example.backend.entity.Mail;
import java.time.LocalDateTime;


@Service
public class MailServiceImpl implements MailService {


    @Autowired
    private MailMapper mailMapper;

    @Autowired
    private UserMapper userMapper;

    //邮件发送器
    @Autowired
    private JavaMailSender mailSender;

    //服务器
    @Autowired
    private GreenMail greenMail;

    //发送方
    @Value("${spring.mail.username}")
    private String from;

    //发送无附件邮件
    public void sendMail(String to, String subject, String content) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, false);
        helper.setFrom(from);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(content, false);
        mailSender.send(message);
    }

    //发送带附件邮件


    //解析邮件
    public void parseMail(Message message) throws Exception {
        Mail mail = new Mail();

        // 解析发件人
        Address[] from = message.getFrom();
        String senderEmail = from.length > 0 ? from[0].toString() : "unknown@example.com";
        User sender = userMapper.findByEmail(senderEmail);
        mail.setSender_id(sender.getId());

        // 解析收件人
        Address[] to = message.getRecipients(Message.RecipientType.TO);
        String receiverEmail = to.length > 0 ? to[0].toString() : "unknown@example.com";
        User receiver = userMapper.findByEmail(receiverEmail);
        mail.setReceiver_id(receiver.getId());

        // 解析邮件主题
        mail.setSubject(message.getSubject());

        // 解析邮件内容
        Object content = message.getContent();

        // 打印邮件内容类型
        System.out.println("Content-Type: " + message.getContentType());

        if (content instanceof Multipart) {
            Multipart multipart = (Multipart) content;
            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart bodyPart = multipart.getBodyPart(i);
                String contentType = bodyPart.getContentType();
                // 检查内容类型并提取文本
                if (contentType.contains("text/plain")) {
                    System.out.println("Plain Text Content:");
                    System.out.println(bodyPart.getContent());
                } else if (contentType.contains("text/html")) {
                    System.out.println("HTML Content:");
                    System.out.println(bodyPart.getContent());
                }
                else{
                    System.out.println(bodyPart.getContent().toString());
                }
            }
        } else if (content instanceof String) {
            // 如果邮件是单部分文本
            mail.setContent(content.toString());
        }
        mail.setCreate_at(LocalDateTime.now());
        mailMapper.insertMail(mail);
    }


}
