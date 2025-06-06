package com.example.backend.Server.Handler;

import com.example.backend.entity.Mail;
import com.example.backend.entity.User;
import com.example.backend.mapper.MailMapper;
import com.example.backend.mapper.UserMapper;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import jakarta.mail.*;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Scope("prototype")
public class SmtpServerHandler extends SimpleChannelInboundHandler<String> {

    private boolean dataMode = false;
    private StringBuilder mailContent = new StringBuilder();
    private List<String> recipients = new ArrayList<>();
    private String sender;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private MailMapper mailMapper;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        System.out.println("SMTP Received: " + msg);
        String[] lines = msg.split("\\r?\\n");
        for (String line : lines) {
            String command = line.trim();

            if (dataMode) {
                if (command.equals(".")) {
                    dataMode = false;

                    try {
                        parseMimeMessage(mailContent.toString());
                    } catch (Exception e) {
                        System.err.println("Error parsing MIME message: " + e.getMessage());
                    }

                    mailContent.setLength(0);
                    recipients.clear();
                    sender = null;
                    ctx.writeAndFlush("250 OK: queued as 12345\r\n");
                } else {
                    mailContent.append(command).append("\r\n");
                    ctx.writeAndFlush("250 OK: successfully getting content");
                }
            } else {
                if (command.startsWith("HELO") || command.startsWith("EHLO")) {
                    String domain = command.split("\\s+", 2)[1];
                    ctx.writeAndFlush("250 Hello " + domain + "\r\n");
                } else if (command.startsWith("MAIL FROM:")) {
                    sender = extractEmail(command);
                    ctx.writeAndFlush("250 OK\r\n");
                } else if (command.startsWith("RCPT TO:")) {
                    String recipient = extractEmail(command);
                    recipients.add(recipient);
                    ctx.writeAndFlush("250 OK\r\n");
                } else if (command.equalsIgnoreCase("DATA")) {
                    dataMode = true;
                    ctx.writeAndFlush("354 Start mail input; end with <CRLF>.<CRLF>\r\n");
                } else if (command.equalsIgnoreCase("QUIT")) {
                    ctx.writeAndFlush("221 Bye\r\n");
                    ctx.close();
                } else if (command.equalsIgnoreCase("RSET")) {
                    mailContent.setLength(0);
                    recipients.clear();
                    sender = null;
                    dataMode = false;
                    ctx.writeAndFlush("250 OK\r\n");
                } else {
                    ctx.writeAndFlush("500 Syntax error\r\n");
                }
            }
        }
    }

    private String extractEmail(String command) {
        Pattern pattern = Pattern.compile("<([^>]+)>");
        Matcher matcher = pattern.matcher(command);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return command.split("\\s+", 2)[1].trim();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }


    //邮件解析
    private void parseMimeMessage(String mimeMessage) throws Exception {
        Properties props = new Properties();
        props.put("mail.smtp.ssl.trust", "false");

        Session session = Session.getInstance(props, null);
        MimeMessage message = new MimeMessage(session, new ByteArrayInputStream(mimeMessage.getBytes()));

        Mail mail = new Mail();

        Address[] from = message.getFrom();
        String senderEmail = from.length > 0 ? from[0].toString() : "unknown@example.com";
        User sender = userMapper.findByEmail(senderEmail);
        mail.setSender_id(sender != null ? sender.getId() : null);

        Address[] to = message.getRecipients(Message.RecipientType.TO);
        String receiverEmail = to != null && to.length > 0 ? to[0].toString() : "unknown@example.com";
        User receiver= userMapper.findByEmail(receiverEmail);
        mail.setReceiver_id(receiver != null ? receiver.getId() : null);

        // 解决中文乱码
        String subject=new String(message.getSubject().getBytes("ISO8859_1"), StandardCharsets.UTF_8);
        mail.setSubject(subject);

        Object content = message.getContent();

        if (content instanceof Multipart) {
            Multipart multipart = (Multipart) content;
            StringBuilder contentBuilder = new StringBuilder();

            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart bodyPart = multipart.getBodyPart(i);
                String contentType = bodyPart.getContentType();

                if (contentType.contains("text/plain")) {
                    String textContent = (String) bodyPart.getContent();
                    contentBuilder.append(textContent);
                } else if (contentType.contains("text/html")) {
                    String htmlContent = (String) bodyPart.getContent();
                    contentBuilder.append(htmlContent);
                } else {
                    contentBuilder.append("[Non-text content: ").append(contentType).append("]");
                }
            }
            mail.setContent(contentBuilder.toString());
        } else if (content instanceof String) {
            // 去除末尾的 \r\n
            mail.setContent(content.toString().replace("\r\n", ""));
        }
        mail.setCreate_at(LocalDateTime.now());
        mailMapper.insertMail(mail);
    }
}