package com.example.backend.service.Impl;


import com.example.backend.Client.ImapClient;
import com.example.backend.Client.SmtpClient;
import com.example.backend.DTO.MailDTO;
import com.example.backend.service.MailService;
import com.example.backend.utils.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


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


    // SSE 端点实现
    @Override
    public SseEmitter streamEmails() {
        SseEmitter emitter = new SseEmitter();
        imapClient.setSseEmitter(emitter);
        return emitter;
    }


    @Override
    public ResultVo sendMail(String to, String subject, String content, List<MultipartFile> attachmentFiles) {
        try {
            this.smtpClient.connect();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ResultVo.fail(0, "Failed to connect to SMTP server" + e.getMessage());
        }
        List<String> attachmentNames = null;
        List<byte[]> attachmentContents = null;
        if (attachmentFiles != null) {
            attachmentNames = new ArrayList<>();
            attachmentContents = new ArrayList<>();
            for (MultipartFile file : attachmentFiles) {
                attachmentNames.add(file.getOriginalFilename());
                try {
                    attachmentContents.add(file.getBytes());
                } catch (IOException e) {
                    return ResultVo.fail(0, "File byte stream error" + e.getMessage());
                }
            }
        }
        try {
            if (attachmentNames != null && attachmentContents != null && attachmentNames.size() == attachmentContents.size()) {
                // 发送带有多个附件的邮件
                smtpClient.sendMail(
                        userEmail, // 发件人地址
                        to,                   // 收件人地址
                        subject,              // 邮件主题
                        content,              // 邮件正文（纯文本部分）
                        null, //"<h1>" + content + "</h1>", // 邮件正文（HTML部分）
                        attachmentNames,      // 附件名称列表
                        attachmentContents    // 附件内容列表
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
            return ResultVo.success("Email sent successfully!");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ResultVo.fail(0, "Failed to send email: " + e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            // 关闭连接
            smtpClient.disconnect();;
        }
    }


    @Override
    public ResultVo fetchMail(long mailId, String mailbox) {
        try {
            this.imapClient.connect();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ResultVo.fail(0, "Failed to connect to IMAP server" + e.getMessage());
        }
        try {
            imapClient.loginCommand(userEmail, userPassword);
        } catch (InterruptedException e) {
            imapClient.disconnect();
            return ResultVo.fail(0, "Failed to send LOGIN command" + e.getMessage());
        }
        MailDTO mailDTO;
        try {
            mailDTO = imapClient.fetchCommand("DETAIL", mailId);
            return ResultVo.success("Mail fetch successfully", mailDTO);
        } catch (InterruptedException e) {
            return ResultVo.fail(0, "Failed to send FETCH command" + e.getMessage());
        } finally {
            imapClient.disconnect();
        }
    }


    @Override
    public ResultVo viewMail(String mailbox, int pageNum, int pageSize){
        List<Long> mailId = null;
        List<MailDTO> mails = null;
        try {
            this.imapClient.connect();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ResultVo.fail(0, "Failed to connect to IMAP server" + e.getMessage());
        }
        try {
            imapClient.loginCommand(userEmail, userPassword);
        } catch (InterruptedException e) {
            imapClient.disconnect();
            return ResultVo.fail(0, "Failed to send LOGIN command" + e.getMessage());
        }
        try {
            imapClient.selectCommand(mailbox);
        } catch (InterruptedException e) {
            imapClient.disconnect();
            return ResultVo.fail(0, "Failed to send SELECT command" + e.getMessage());
        }
        try {
            mailId = imapClient.searchCommand(null, null, null, null, null, null, false, false);
        } catch (InterruptedException e) {
            imapClient.disconnect();
            return ResultVo.fail(0, "Failed to send SEARCH command" + e.getMessage());
        }
        try {
            if(mailId != null) {
                mails = new ArrayList<>();
                for(long id : mailId) {
                    System.out.println(id);
                    mails.add(imapClient.fetchCommand("SIMPLE", id));
                }
                return ResultVo.success("Mail fetch successfully", mails);
            } else {
                return ResultVo.fail(0, "No mail in the mailbox");
            }
        } catch (InterruptedException e) {
            return ResultVo.fail(0, "Failed to send FETCH command" + e.getMessage());
        } finally {
            imapClient.disconnect();
        }
    }


    @Override
    public ResultVo searchMail(String mailbox, int pageNum, int pageSize, String from, String to, String subject, String body, int since, String unseen, boolean sender_star, boolean receiver_star) {
        List<Long> mailId = null;
        List<MailDTO> mails = null;
        LocalDateTime dateTime;
        if (since > 0) {
            dateTime = LocalDateTime.now().minusDays(since).withHour(0).withMinute(0).withSecond(0);
        }
        else {
            dateTime = null;
        }
        try {
            this.imapClient.connect();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ResultVo.fail(0, "Failed to connect to IMAP server" + e.getMessage());
        }
        try {
            imapClient.loginCommand(userEmail, userPassword);
        } catch (InterruptedException e) {
            imapClient.disconnect();
            return ResultVo.fail(0, "Failed to send LOGIN command" + e.getMessage());
        }
        try {
            imapClient.selectCommand(mailbox);
        } catch (InterruptedException e) {
            imapClient.disconnect();
            return ResultVo.fail(0, "Failed to send SELECT command" + e.getMessage());
        }
        try {
            mailId = imapClient.searchCommand(from, to, subject, body, dateTime, unseen, sender_star, receiver_star);
        } catch (InterruptedException e) {
            imapClient.disconnect();
            return ResultVo.fail(0, "Failed to send SEARCH command" + e.getMessage());
        }
        try {
            if(mailId != null) {
                mails = new ArrayList<>();
                for(long id : mailId) {
                    System.out.println(id);
                    mails.add(imapClient.fetchCommand("SIMPLE", id));
                }
                return ResultVo.success("Mail search successfully", mails);
            } else {
                return ResultVo.fail(0, "No mail searched in this mailbox");
            }
        } catch (InterruptedException e) {
            return ResultVo.fail(0, "Failed to send SEARCH command" + e.getMessage());
        } finally {
            imapClient.disconnect();
        }
    }


    @Override
    public ResultVo changeMail(long mailId, String sign, String op) {
        try {
            this.imapClient.connect();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ResultVo.fail(0, "Failed to connect to IMAP server" + e.getMessage());
        }
        try {
            imapClient.loginCommand(userEmail, userPassword);
        } catch (InterruptedException e) {
            imapClient.disconnect();
            return ResultVo.fail(0, "Failed to send LOGIN command" + e.getMessage());
        }
        try {
            imapClient.storeCommand(mailId, sign, op);
            return ResultVo.success("Mail stored successfully");
        } catch (InterruptedException e) {
            return ResultVo.fail(0, "Failed to send STORE command" + e.getMessage());
        } finally {
            imapClient.disconnect();
        }
    }


    @Override
    public ResultVo deleteMail(long mailId) {
        try {
            this.imapClient.connect();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ResultVo.fail(0, "Failed to connect to IMAP server" + e.getMessage());
        }
        try {
            imapClient.loginCommand(userEmail, userPassword);
        } catch (InterruptedException e) {
            imapClient.disconnect();
            return ResultVo.fail(0, "Failed to send LOGIN command" + e.getMessage());
        }
        try {
            imapClient.deleteCommand(mailId);
            return ResultVo.success("Mail deleted successfully");
        } catch (InterruptedException e) {
            return ResultVo.fail(0, "Failed to send DELETE command" + e.getMessage());
        } finally {
            imapClient.disconnect();
        }
    }


    @Override
    public ResultVo draft(long mailId, String to, String subject, String content) {
        try {
            this.imapClient.connect();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ResultVo.fail(0, "Failed to connect to IMAP server" + e.getMessage());
        }
        try {
            imapClient.loginCommand(userEmail, userPassword);
        } catch (InterruptedException e) {
            imapClient.disconnect();
            return ResultVo.fail(0, "Failed to send LOGIN command" + e.getMessage());
        }
        try {
            imapClient.draftCommand(mailId, userEmail, to, subject, content);
            return ResultVo.success("Draft saved successfully");
        } catch (InterruptedException e) {
            return ResultVo.fail(0, "Failed to send DRAFT command" + e.getMessage());
        } finally {
            imapClient.disconnect();
        }
    }
}
