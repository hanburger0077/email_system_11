package com.example.backend.DTO;

import com.example.backend.entity.Mail;
import com.example.backend.entity.User;
import com.example.backend.mapper.UserMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Component
public class MailDTO implements Serializable {
    private long mail_id;
    private String sender_email;
    private String receiver_email;
    private String subject;
    private String content;
    private LocalDateTime create_at;
    private short sender_sign;
    private short receiver_sign;
    private short read;
    private short sender_star;
    private short receiver_star;
    private List<Long> attachmentIds;


    public MailDTO (Mail mail, UserMapper userMapper, List<Long> attachmentIds) {
        mail_id = mail.getMail_id();
        User sender = userMapper.findById(mail.getSender_id());
        sender_email = sender.getEmail();
        User receiver = userMapper.findById(mail.getReceiver_id());
        receiver_email = receiver.getEmail();
        subject = mail.getSubject();
        content = mail.getContent();
        create_at = mail.getCreate_at();
        sender_sign = mail.getSender_sign();
        receiver_sign = mail.getReceiver_sign();
        read = mail.getRead();
        sender_star = mail.getSender_star();
        receiver_star = mail.getReceiver_star();
        if (attachmentIds != null && !attachmentIds.isEmpty()) {
           this.attachmentIds = attachmentIds;
        } else {
            attachmentIds = null;
        }
    }

    public String getMailDetail() {
        StringBuilder s = new StringBuilder();
        s.append(content).append("\r\n");
        s.append("ID ").append(mail_id).append("\r\n");
        s.append("FROM ").append(sender_email).append("\r\n");
        s.append("TO ").append(receiver_email).append("\r\n");
        s.append("SUBJECT ").append(subject).append("\r\n");
        s.append("DATE ").append(create_at.toString()).append("\r\n");
        s.append("READ ").append(read).append("\r\n");
        s.append("S_STAR ").append(sender_star).append("\r\n");
        s.append("R_STAR ").append(receiver_star).append("\r\n");
        if (attachmentIds != null && !attachmentIds.isEmpty()) {
            s.append("ATTACHMENT");
            for (long attachmentId : attachmentIds) {
                s.append(" ").append(attachmentId);
            }
        }
        return s.toString();
    }

    public int getMailDetailLength() {
        int length = 0;
        length += String.valueOf(mail_id).length();
        length += sender_email.length();
        length += receiver_email.length();
        length += subject.length();
        length += content.length();
        length += create_at.toString().length();
        length += String.valueOf(read).length();
        length += String.valueOf(sender_star).length();
        length += String.valueOf(receiver_star).length();
        if (attachmentIds != null && !attachmentIds.isEmpty()) {
            for (long attachmentId : attachmentIds) {
                length += String.valueOf(attachmentId).length() + 1;
            }
            length -= 1;
        }
        return length;
    }

    public String getMailSimple() {
        StringBuilder s = new StringBuilder();
        s.append(content, 0, Math.min(content.length(), 20)).append("\r\n");
        s.append("ID ").append(mail_id).append("\r\n");
        s.append("FROM ").append(sender_email).append("\r\n");
        s.append("TO ").append(receiver_email).append("\r\n");
        s.append("SUBJECT ").append(subject).append("\r\n");
        s.append("DATE ").append(create_at.toString()).append("\r\n");
        s.append("READ ").append(read).append("\r\n");
        s.append("S_STAR ").append(sender_star).append("\r\n");
        s.append("R_STAR ").append(receiver_star);
        return s.toString();
    }

    public int getMailSimpleLength() {
        int length = 0;
        length += String.valueOf(mail_id).length();
        length += sender_email.length();
        length += receiver_email.length();
        length += subject.length();
        length += create_at.toString().length();
        length += String.valueOf(read).length();
        length += String.valueOf(sender_star).length();
        length += String.valueOf(receiver_star).length();
        return length;
    }
}
