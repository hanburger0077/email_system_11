package com.example.backend.service;


import com.example.backend.utils.ResultVo;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.util.List;

public interface MailService {
    //
    SseEmitter streamEmails();
    //
    public ResultVo loginIMAP(String username, String password);
    //
    public ResultVo sendMail(String to, String subject, String content, List<MultipartFile> attachmentFiles);
    //
    public ResultVo fetchMail(long mailId, String mailbox);

    public ResultVo viewMail(String mailbox, int pageNum, int pageSize);

    public ResultVo searchMail(String mailbox, String from, String to, String subject, String body, int since, String unseen, boolean sender_star, boolean receiver_star);
    public ResultVo searchMail(String mailbox, int pageNum, int pageSize, String from, String to, String subject, String body, int since, String unseen, boolean sender_star, boolean receiver_star);

    public ResultVo changeMail(long mailId, String sign, String op);

    public ResultVo deleteMail(long mailId);

    public ResultVo draft(long mailId, String to, String subject, String content);

    public ResultVo disconnect();
}
