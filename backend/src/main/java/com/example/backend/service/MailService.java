package com.example.backend.service;


import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

public interface MailService {
    //
    SseEmitter streamEmails();
    //
    public String sendMail(String to, String subject, String content, List<MultipartFile> attachmentFiles);
    //
    public String fetchMail(long mailId, String mailbox);

    public String viewMail(String mailbox, int pageNum, int pageSize);

    public String searchMail(String mailbox, int pageNum, int pageSize, String from, String to, String subject, String body, int since, String unseen, boolean sender_star, boolean receiver_star);

    public void changeMail(long mailId, String sign, String op);

    public void deleteMail(long mailId);

    public void draft(long mailId, String to, String subject, String content);
}
