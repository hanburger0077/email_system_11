package com.example.backend.controller;


import com.example.backend.service.MailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class MailController {

    @Autowired
    private MailService mailService;

    @RequestMapping("/send-mail")
    public String sendMail(
            @RequestParam String to,
            @RequestParam String subject,
            @RequestParam String content,
            @RequestParam(required = false) String attachmentName,
            @RequestParam(required = false) byte[] attachmentContent) {
        return mailService.sendMail(to, subject, content, attachmentName, attachmentContent);
    }

    @RequestMapping("/fetch-mail")
    public String fetchMail(
            @RequestParam long mailId){
        return mailService.fetchMail(mailId);
    }
}
