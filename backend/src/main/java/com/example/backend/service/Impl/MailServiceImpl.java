package com.example.backend.service.Impl;


import com.example.backend.Client.ImapClient;
import com.example.backend.Client.SmtpClient;
import com.example.backend.DTO.MailDTO;
import com.example.backend.service.MailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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

    public String sendMail(String to, String subject, String content, String attachmentName, byte[] attachmentContent){
        try {
            this.smtpClient.connect();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to connect to SMTP server", e);
        }
        try {
            if (attachmentName != null && attachmentContent != null) {
                // 发送带有附件的邮件
                smtpClient.sendMail(
                        userEmail, // 发件人地址
                        to,                   // 收件人地址
                        subject,              // 邮件主题
                        content,              // 邮件正文（纯文本部分）
                        "<h1>" + content + "</h1>", // 邮件正文（HTML部分）
                        attachmentName,       // 附件名称
                        attachmentContent     // 附件内容
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
            return "Email sent successfully!";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "Failed to send email: " + e.getMessage();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            // 关闭连接
            smtpClient.disconnect();
        }
    }


    public String fetchMail(long mailId, String mailbox) {
        try {
            this.imapClient.connect();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to connect to IMAP server", e);
        }
        try {
            imapClient.loginCommand(userEmail, userPassword);
        } catch (InterruptedException e) {
            imapClient.disconnect();
            throw new RuntimeException("Failed to send Login command", e);
        }
        MailDTO mailDTO;
        try {
            mailDTO = imapClient.fetchCommand("DETAIL", mailId);
            //测试字段
            String mailStr = "发件人：\t" + mailDTO.getSender_email() + "<br>"
                    + "收件人：\t" + mailDTO.getReceiver_email() + "<br>"
                    + "主题：\t" + mailDTO.getSubject() + "<br>"
                    + "内容：\t" + mailDTO.getContent() + "<br>"
                    + "时间：\t" + mailDTO.getCreate_at().toString() + "<br>"
                    + "读取：\t" + mailDTO.getRead() + "<br>";
            String starType = null;
            if (mailbox.equals("SENT")) {
                mailStr = mailStr + "S星：\t" + mailDTO.getSender_star() + "<br>";
                starType = "S_STAR";
            } else if (mailbox.equals("INBOX")) {
                mailStr = mailStr + "R星：\t" + mailDTO.getReceiver_star() + "<br>";
                starType = "R_STAR";
            }
            if (mailbox.equals("INBOX")) {
                mailStr += "<body>" +
                        "    <a href=\"/view-mail/" + mailbox + "/fetch-mail/" + mailId + "/change-mail/READ/+FLAG\">" +
                        "        <button>标记为已读</button>" +
                        "    </a>" +
                        "</body>"
                        + "<body>" +
                        "    <a href=\"/view-mail/" + mailbox + "/fetch-mail/" + mailId + "/change-mail/READ/-FLAG\">" +
                        "        <button>标记为未读</button>\n" +
                        "    </a>" +
                        "</body>";
            }
            if (mailbox.equals("INBOX") || mailbox.equals("SENT")) {
                mailStr += "<body>" +
                        "    <a href=\"/view-mail/" + mailbox + "/fetch-mail/" + mailId + "/change-mail/" + starType + "/+FLAG\">" +
                        "        <button>标记为星标</button>" +
                        "    </a>" +
                        "</body>"
                        + "<body>" +
                        "    <a href=\"/view-mail/" + mailbox + "/fetch-mail/" + mailId + "/change-mail/" + starType + "/-FLAG\">" +
                        "        <button>取消星标</button>" +
                        "    </a>" +
                        "</body>";
            }
            if (mailbox.equals("INBOX")) {
                mailStr += "<body>" +
                        "    <a href=\"/view-mail/" + mailbox + "/fetch-mail/" + mailId + "/change-mail/TRASH/+FLAG\">" +
                        "        <button>放入回收站</button>" +
                        "    </a>" +
                        "</body>";
            }
            if (mailbox.equals("TRASH")) {
                mailStr += "<body>" +
                        "    <a href=\"/view-mail/" + mailbox + "/fetch-mail/" + mailId + "/change-mail/TRASH/-FLAG\">" +
                        "        <button>从回收站恢复</button>" +
                        "    </a>" +
                        "</body>";
            }
            //测试字段
            return mailStr.replace("\n","<br>");
        } catch (InterruptedException e) {
            throw new RuntimeException("Failed to send FETCH command",e);
        } finally {
            imapClient.disconnect();
        }
    }


    public String viewMail(String mailbox, int pageNum, int pageSize){
        List<Long> mailId = null;
        List<MailDTO> mails = null;
        try {
            this.imapClient.connect();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to connect to IMAP server", e);
        }
        try {
            imapClient.loginCommand(userEmail, userPassword);
        } catch (InterruptedException e) {
            imapClient.disconnect();
            throw new RuntimeException("Failed to send Login command", e);
        }
        try {
            imapClient.selectCommand(mailbox);
        } catch (InterruptedException e) {
            imapClient.disconnect();
            throw new RuntimeException("Failed to send SELECT command", e);
        }
        try {
            mailId = imapClient.searchCommand(null, null, null, null, null, null, false, false);
        } catch (InterruptedException e) {
            imapClient.disconnect();
            throw new RuntimeException("Failed to send SEARCH command", e);
        }
        try {
            if(mailId != null) {
                mails = new ArrayList<>();
                for(long id : mailId) {
                    System.out.println(id);
                    mails.add(imapClient.fetchCommand("SIMPLE", id));
                }
            }
            StringBuffer s = new StringBuffer("");
            s.append("<form action=\"/search-mail/" + mailbox + "/1" + "\">");
            if (mailbox.equals("INBOX") || mailbox.equals("TRASH") || mailbox.equals("JUNK")) {
                s.append("from:<input type=\"text\" name=\"from\" >  <br>");
            }
            else if (mailbox.equals("SENT") || mailbox.equals("DRAFT")) {
                s.append("to:<input type=\"text\" name=\"to\" >  <br>");
            }
            s.append("subject:<input type=\"text\" name=\"subject\"> <br>" +
                    "body:<input type=\"text\" name=\"body\"> <br>" +
                    "since:<input type=\"text\" name=\"since\"> <br>");
            if (mailbox.equals("INBOX") || mailbox.equals("TRASH") || mailbox.equals("JUNK")) {
                s.append("unseen:<input type=\"text\" name=\"unseen\"> <br>" +
                        "receiver_star:<input type=\"text\" name=\"receiver_star\"> <br>");
            }
            else if (mailbox.equals("SENT") || mailbox.equals("DRAFT")) {
                s.append("sender_star:<input type=\"text\" name=\"sender_star\"> <br>");
            }
            s.append("<input type=\"submit\" ></form>");
            for(MailDTO mailDTO : mails) {
                s.append(mailDTO.getSender_email()).append("&ensp;&ensp;")
                        .append(mailDTO.getReceiver_email()).append("&ensp;&ensp;")
                        .append(mailDTO.getSubject()).append("&ensp;&ensp;")
                        .append(mailDTO.getContent()).append("……&ensp;&ensp;")
                        .append(mailDTO.getCreate_at().toString()).append("    ")
                        .append("<a href=\"/view-mail/" + mailbox + "/fetch-mail/" + mailDTO.getMail_id() + "\">详情</a>").append("<br>");
            }
            return s.toString();
        } catch (InterruptedException e) {
            throw new RuntimeException("Failed to send SEARCH command", e);
        } finally {
            imapClient.disconnect();
        }
    }

    public String searchMail(String mailbox, int pageNum, int pageSize, String from, String to, String subject, String body, int since, String unseen, boolean sender_star, boolean receiver_star) {
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
            throw new RuntimeException("Failed to connect to IMAP server", e);
        }
        try {
            imapClient.loginCommand(userEmail, userPassword);
        } catch (InterruptedException e) {
            imapClient.disconnect();
            throw new RuntimeException("Failed to send Login command", e);
        }
        try {
            imapClient.selectCommand(mailbox);
        } catch (InterruptedException e) {
            imapClient.disconnect();
            throw new RuntimeException("Failed to send SELECT command", e);
        }
        try {
            mailId = imapClient.searchCommand(from, to, subject, body, dateTime, unseen, sender_star, receiver_star);
        } catch (InterruptedException e) {
            imapClient.disconnect();
            throw new RuntimeException("Failed to send SEARCH command", e);
        }
        try {
            if(mailId != null) {
                mails = new ArrayList<>();
                for(long id : mailId) {
                    System.out.println(id);
                    mails.add(imapClient.fetchCommand("SIMPLE", id));
                }
            }
            //测试模块
            StringBuffer s = new StringBuffer("");
            for(MailDTO mailDTO : mails) {
                s.append(mailDTO.getSender_email()).append("&ensp;&ensp;")
                        .append(mailDTO.getReceiver_email()).append("&ensp;&ensp;")
                        .append(mailDTO.getSubject()).append("&ensp;&ensp;")
                        .append(mailDTO.getContent()).append("……&ensp;&ensp;")
                        .append(mailDTO.getCreate_at().toString()).append("    ")
                        .append("<a href=\"/view-mail/" + mailbox + "/fetch-mail/" + mailDTO.getMail_id() + "\">详情</a>").append("<br>");
            }
            //测试模块
            return s.toString();
        } catch (InterruptedException e) {
            throw new RuntimeException("Failed to send SEARCH command", e);
        } finally {
            imapClient.disconnect();
        }
    }

    public void changeMail(long mailId, String sign, String op) {
        try {
            this.imapClient.connect();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to connect to IMAP server", e);
        }
        try {
            imapClient.loginCommand(userEmail, userPassword);
        } catch (InterruptedException e) {
            imapClient.disconnect();
            throw new RuntimeException("Failed to send Login command", e);
        }
        MailDTO mailDTO;
        try {
            imapClient.storeCommand(mailId, sign, op);
        } catch (InterruptedException e) {
            throw new RuntimeException("Failed to send FETCH command",e);
        } finally {
            imapClient.disconnect();
        }
    }
}
