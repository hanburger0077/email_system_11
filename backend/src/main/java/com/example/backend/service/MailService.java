package com.example.backend.service;

import jakarta.mail.Message;
import jakarta.mail.MessagingException;

public interface MailService {
    void sendMail(String to, String subject, String content) throws MessagingException;
    void parseMail(Message message) throws Exception;
}
