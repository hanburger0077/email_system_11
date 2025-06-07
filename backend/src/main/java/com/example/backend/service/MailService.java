package com.example.backend.service;


public interface MailService {

    public String sendMail(String to, String subject, String content, String attachmentName, byte[] attachmentContent);

    public String fetchMail(long mailId);

}
