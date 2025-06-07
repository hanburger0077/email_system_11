package com.example.backend.DTO;

import com.example.backend.entity.Mail;
import com.example.backend.entity.User;
import com.example.backend.mapper.UserMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Component
public class MailDTO {
    private String sender_email;
    private String receiver_email;
    private String subject;
    private String content;
    private LocalDateTime create_at;

    public MailDTO (Mail mail, UserMapper userMapper){
        User sender = userMapper.findById(mail.getSender_id());
        sender_email = sender.getEmail();
        User receiver = userMapper.findById(mail.getReceiver_id());
        receiver_email = receiver.getEmail();
        subject = mail.getSubject();
        content = mail.getContent();
        create_at = mail.getCreate_at();
    }

    public String getMailString() {
        StringBuilder s = new StringBuilder();
        s.append(content).append("\r\n");
        s.append("FROM ").append(sender_email).append("\r\n");
        s.append("TO ").append(receiver_email).append("\r\n");
        s.append("SUBJECT ").append(subject).append("\r\n");
        s.append("DATE ").append(create_at.toString());
        return s.toString();
    }

    public int getMailStringLength() {
        int length = 0;
        length += sender_email.length();
        length += receiver_email.length();
        length += subject.length();
        length += content.length();
        length += create_at.toString().length();
        return length;
    }
}
