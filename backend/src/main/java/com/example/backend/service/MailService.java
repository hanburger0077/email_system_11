package com.example.backend.service;



import com.icegreen.greenmail.util.GreenMail;
import jakarta.mail.*;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.example.backend.dao.MailDao;
import com.example.backend.entity.Mail;

import java.time.LocalDateTime;


@Service
public class MailService {


    @Autowired
    private final MailDao mailDao;

    //邮件发送器
    @Autowired
    private JavaMailSender mailSender;

    //服务器
    @Autowired

    //发送方
    @Value("${spring.mail.username}")
    private String from;

    public MailService(MailDao mailDao) {
        this.mailDao= mailDao;
    }

    //将邮件插入数据库
    public void createMail(Mail mail) {
        mailDao.insertMail(mail);
    }

    //保存邮件
    public void saveMail(Long senderId, Long receiverId, String subject, String content) {
        Mail mail = new Mail();
        mail.setSender_id(senderId);
        mail.setReceiver_id(receiverId);
        mail.setSubject(subject);
        mail.setContent(content);
        mail.setCreate_at(LocalDateTime.now());
        mailDao.insertMail(mail);
    }

    //发送邮件
    public void sendMail(String to, String subject, String content) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.setFrom(from);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(content, true);
        saveMail(Long.parseLong(from),Long.parseLong(to),subject,content);
        mailSender.send(message);
    }

    //接收邮件



}
