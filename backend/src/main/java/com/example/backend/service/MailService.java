package com.example.backend.service;


import java.time.LocalDateTime;

public interface MailService {

    public String sendMail(String to, String subject, String content, String attachmentName, byte[] attachmentContent);
    //
    public String fetchMail(long mailId, String mailbox);

    public String viewMail(String mailbox, int pageNum, int pageSize);

    public String searchMail(String mailbox, int pageNum, int pageSize, String from, String to, String subject, String body, int since, String unseen, boolean sender_star, boolean receiver_star);

    public void changeMail(long mailId, String sign, String op);

}
