package com.example.backend.controller;


import com.example.backend.service.impl.MailServiceImpl;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class MailController {

    @Autowired
    private MailServiceImpl mailService;

    @RequestMapping("/send-mail")
    public String sendMail(String to, String subject, String content) throws MessagingException {
        mailService.sendMail(to, subject, content);
        return "Email sent successfully!";
    }
}
