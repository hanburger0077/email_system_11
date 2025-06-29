package com.example.backend.service.Impl;


import com.example.backend.Client.ImapClient;
import com.example.backend.Client.Manager.ImapClientManager;
import com.example.backend.Client.SmtpClient;
import com.example.backend.DTO.MailDTO;
import com.example.backend.service.MailService;
import com.example.backend.utils.ResultVo;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.context.annotation.RequestScope;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Service
@RequestScope
public class MailServiceImpl implements MailService {

    // 用户邮箱
    private String userEmail;

    // 用户密码
    private String userPassword;

    @Autowired
    private final SmtpClient smtpClient;

    @Autowired
    private final ImapClientManager imapClientManager;

    private ImapClient imapClient;

    public MailServiceImpl(SmtpClient smtpClient, ImapClientManager imapClientManager,  @Autowired(required = false) HttpServletRequest request) {
        this.smtpClient = smtpClient;
        this.imapClientManager = imapClientManager;
        userEmail = extractUserEmailFromRequest(request);
        imapClient = imapClientManager.getClientForUser(userEmail);
    }


    private String extractUserEmailFromRequest(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("loginUserEmail".equals(cookie.getName()) && StringUtils.hasText(cookie.getValue())) {
                    System.out.println(cookie.getValue());
                    return cookie.getValue();
                }
            }
        }
        return null;
    }


    // SSE 端点实现
    @Override
    public SseEmitter streamEmails() {
        SseEmitter emitter = new SseEmitter(60 * 60 * 1000L); // 1小时超时
        imapClient.setSseEmitter(emitter);
        System.out.println("build!!!");
        return emitter;
    }


    @Override
    public ResultVo loginIMAP(String username, String password) {
        try {
            userEmail = username;
            userPassword = password;
            if (!imapClient.isConnected()) {
                imapClient.connect();
            }

            imapClient.loginCommand(username, password);
            return ResultVo.success("LOGIN and IDLE successfully");
        } catch (Exception e) {
            return ResultVo.fail("操作失败", "Failed to login: " + e.getMessage());
        }
    }


    @Override
    public ResultVo sendMail(String to, String subject, String content, List<MultipartFile> attachmentFiles) {
        try {
            this.smtpClient.connect();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ResultVo.fail("操作失败", "Failed to connect to SMTP server" + e.getMessage());
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
                    return ResultVo.fail("操作失败", "File byte stream error" + e.getMessage());
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
            return ResultVo.fail("操作失败", "Failed to send email: " + e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            // 关闭连接
            smtpClient.disconnect();;
        }
    }


    @Override
    public ResultVo fetchMail(long mailId, String mailbox) {
        MailDTO mailDTO;
        try {
            imapClient.doneCommand();
        } catch (InterruptedException e) {
            // 强行恢复空闲
            try {
                imapClient.idleCommand();
            } catch (InterruptedException ee) {
                throw new RuntimeException(ee);
            }
            return ResultVo.fail("操作失败", "Failed to send DONE command" + e.getMessage());
        }
        try {
            mailDTO = imapClient.fetchCommand("DETAIL", mailId);
            return ResultVo.success("Mail fetch successfully", mailDTO);
        } catch (InterruptedException e) {
            return ResultVo.fail("操作失败", "Failed to send FETCH command" + e.getMessage());
        } finally {
            try {
                imapClient.idleCommand();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }


    @Override
    public ResultVo viewMail(String mailbox, int pageNum, int pageSize){
        List<Long> mailId = null;
        List<MailDTO> mails = null;
        List<Long> pageMailIds = null;
        int totalPageNum = 0;
        try {
            imapClient.doneCommand();
        } catch (InterruptedException e) {
            // 强行恢复空闲
            try {
                imapClient.idleCommand();
            } catch (InterruptedException ee) {
                throw new RuntimeException(ee);
            }
            return ResultVo.fail("操作失败", "Failed to send DONE command" + e.getMessage());
        }
        try {
            imapClient.selectCommand(mailbox);
        } catch (InterruptedException e) {
            // 强行恢复空闲
            try {
                imapClient.idleCommand();
            } catch (InterruptedException ee) {
                throw new RuntimeException(ee);
            }
            return ResultVo.fail("操作失败", "Failed to send SELECT command" + e.getMessage());
        }
        try {
            mailId = imapClient.searchCommand(null, null, null, null, null, null, false, false);
            if(mailId != null) {
                // 计算分页的起始和结束索引
                int total = mailId.size();
                int fromIndex = (pageNum - 1) * pageSize;
                int toIndex = Math.min(fromIndex + pageSize, total);
                // 检查分页参数是否有效
                if (fromIndex >= total) {
                    return ResultVo.fail("操作失败", "Requested page is out of range");
                }
                //获取总页数
                totalPageNum = (int)Math.ceil((double) total / pageSize);
                // 获取当前页的邮件ID
                pageMailIds = mailId.subList(fromIndex, toIndex);
            }
        } catch (InterruptedException e) {
            // 强行恢复空闲
            try {
                imapClient.idleCommand();
            } catch (InterruptedException ee) {
                throw new RuntimeException(ee);
            }
            return ResultVo.fail("操作失败", "Failed to send SEARCH command" + e.getMessage());
        }
        try {
            if(mailId != null) {
                mails = new ArrayList<>();
                for(long id : pageMailIds) {
                    mails.add(imapClient.fetchCommand("SIMPLE", id));
                }
                return ResultVo.success(String.valueOf(totalPageNum), mails);
            } else {
                return ResultVo.success("操作成功", new ArrayList<MailDTO>());
            }
        } catch (InterruptedException e) {
            return ResultVo.fail("操作失败", "Failed to send FETCH command" + e.getMessage());
        } finally {
            try {
                imapClient.idleCommand();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }


    @Override
    public ResultVo searchMail(String mailbox, String from, String to, String subject, String body, int since, String unseen, boolean sender_star, boolean receiver_star) {
        List<Long> mailId = null;
        List<MailDTO> mails = null;
        LocalDateTime dateTime;
        int totalPageNum = 0;
        if (since > 0) {
            dateTime = LocalDateTime.now().minusDays(since).withHour(0).withMinute(0).withSecond(0);
        }
        else {
            dateTime = null;
        }
        try {
            imapClient.doneCommand();
        } catch (InterruptedException e) {
            // 强行恢复空闲
            try {
                imapClient.idleCommand();
            } catch (InterruptedException ee) {
                throw new RuntimeException(ee);
            }
            return ResultVo.fail("操作失败", "Failed to send DONE command" + e.getMessage());
        }
        try {
            imapClient.selectCommand(mailbox);
        } catch (InterruptedException e) {
            // 强行恢复空闲
            try {
                imapClient.idleCommand();
            } catch (InterruptedException ee) {
                throw new RuntimeException(ee);
            }
            return ResultVo.fail("操作失败", "Failed to send SELECT command" + e.getMessage());
        }
        try {
            mailId = imapClient.searchCommand(from, to, subject, body, dateTime, unseen, sender_star, receiver_star);
        } catch (InterruptedException e) {
            // 强行恢复空闲
            try {
                imapClient.idleCommand();
            } catch (InterruptedException ee) {
                throw new RuntimeException(ee);
            }
            return ResultVo.fail("操作失败", "Failed to send SEARCH command" + e.getMessage());
        }
        try {
            if(mailId != null) {
                mails = new ArrayList<>();
                for(long id : mailId) {
                    MailDTO mailDTO = imapClient.fetchCommand("SIMPLE", id);
                    
                    mailDTO.setMailbox(mailbox);
                    mails.add(mailDTO);
                }
                return ResultVo.success(String.valueOf(totalPageNum), mails);
            } else {
                return ResultVo.success("操作成功", new ArrayList<>());
            }
        } catch (InterruptedException e) {
            return ResultVo.fail("操作失败", "Failed to send SEARCH command" + e.getMessage());
        } finally {
            try {
                imapClient.idleCommand();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }


    @Override
    public ResultVo searchMail(String mailbox, int pageNum, int pageSize, String from, String to, String subject, String body, int since, String unseen, boolean sender_star, boolean receiver_star) {
        List<Long> mailId = null;
        List<Long> pageMailIds = null;
        List<MailDTO> mails = null;
        LocalDateTime dateTime;
        int totalPageNum = 0;
        if (since > 0) {
            dateTime = LocalDateTime.now().minusDays(since).withHour(0).withMinute(0).withSecond(0);
        }
        else {
            dateTime = null;
        }
        try {
            imapClient.doneCommand();
        } catch (InterruptedException e) {
            // 强行恢复空闲
            try {
                imapClient.idleCommand();
            } catch (InterruptedException ee) {
                throw new RuntimeException(ee);
            }
            return ResultVo.fail("操作失败", "Failed to send DONE command" + e.getMessage());
        }
        try {
            imapClient.selectCommand(mailbox);
        } catch (InterruptedException e) {
            // 强行恢复空闲
            try {
                imapClient.idleCommand();
            } catch (InterruptedException ee) {
                throw new RuntimeException(ee);
            }
            return ResultVo.fail("操作失败", "Failed to send SELECT command" + e.getMessage());
        }
        try {
            mailId = imapClient.searchCommand(from, to, subject, body, dateTime, unseen, sender_star, receiver_star);
            if(mailId != null) {
                // 计算分页的起始和结束索引
                int total = mailId.size();
                int fromIndex = (pageNum - 1) * pageSize;
                int toIndex = Math.min(fromIndex + pageSize, total);
                // 检查分页参数是否有效
                if (fromIndex >= total) {
                    return ResultVo.fail("操作失败", "Requested page is out of range");
                }
                //获取总页数
                totalPageNum = (int)Math.ceil((double) total / pageSize);
                // 获取当前页的邮件ID
                pageMailIds = mailId.subList(fromIndex, toIndex);
            }
        } catch (InterruptedException e) {
            // 强行恢复空闲
            try {
                imapClient.idleCommand();
            } catch (InterruptedException ee) {
                throw new RuntimeException(ee);
            }
            return ResultVo.fail("操作失败", "Failed to send SEARCH command" + e.getMessage());
        }
        try {
            if(mailId != null) {
                mails = new ArrayList<>();
                for(long id : pageMailIds) {
                    mails.add(imapClient.fetchCommand("SIMPLE", id));
                }
                return ResultVo.success(String.valueOf(totalPageNum), mails);
            } else {
                return ResultVo.fail("操作失败", "No mail searched in this mailbox");
            }
        } catch (InterruptedException e) {
            return ResultVo.fail("操作失败", "Failed to send SEARCH command" + e.getMessage());
        } finally {
            try {
                imapClient.idleCommand();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }


    @Override
    public ResultVo changeMail(long mailId, String sign, String op) {
        try {
            imapClient.doneCommand();
        } catch (InterruptedException e) {
            // 强行恢复空闲
            try {
                imapClient.idleCommand();
            } catch (InterruptedException ee) {
                throw new RuntimeException(ee);
            }
            return ResultVo.fail("操作失败", "Failed to send DONE command" + e.getMessage());
        }
        try {
            imapClient.storeCommand(mailId, sign, op);
            return ResultVo.success("Mail stored successfully");
        } catch (InterruptedException e) {
            return ResultVo.fail("操作失败", "Failed to send STORE command" + e.getMessage());
        } finally {
            try {
                imapClient.idleCommand();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }


    @Override
    public ResultVo deleteMail(long mailId) {
        try {
            imapClient.doneCommand();
        } catch (InterruptedException e) {
            // 强行恢复空闲
            try {
                imapClient.idleCommand();
            } catch (InterruptedException ee) {
                throw new RuntimeException(ee);
            }
            return ResultVo.fail("操作失败", "Failed to send DONE command" + e.getMessage());
        }
        try {
            imapClient.deleteCommand(mailId);
            return ResultVo.success("Mail deleted successfully");
        } catch (InterruptedException e) {
            return ResultVo.fail("操作失败", "Failed to send DELETE command" + e.getMessage());
        } finally {
            try {
                imapClient.idleCommand();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }


    @Override
    public ResultVo draft(long mailId, String to, String subject, String content) {
        try {
            imapClient.doneCommand();
        } catch (InterruptedException e) {
            return ResultVo.fail("操作失败", "Failed to send DONE command" + e.getMessage());
        }
        try {
            imapClient.draftCommand(mailId, userEmail, to, subject, content);
            return ResultVo.success("Draft saved successfully");
        } catch (InterruptedException e) {
            return ResultVo.fail("操作失败", "Failed to send DRAFT command" + e.getMessage());
        } finally {
            try {
                imapClient.idleCommand();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }


    @Override
    public ResultVo disconnect() {
        imapClient.disconnect();
        System.out.println("IMAP client disconnect");
        return ResultVo.success("IMAP client disconnect successfully");
    }

}
