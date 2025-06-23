package com.example.backend.Server.Handler;

import com.example.backend.entity.Attachment;
import com.example.backend.entity.Mail;
import com.example.backend.entity.User;
import com.example.backend.mapper.AttachmentMapper;
import com.example.backend.mapper.MailMapper;
import com.example.backend.mapper.UserMapper;
import com.example.backend.service.AttachmentService;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import jakarta.activation.MimetypesFileTypeMap;
import jakarta.mail.*;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Scope("prototype")
public class SmtpServerHandler extends SimpleChannelInboundHandler<String> {

    private boolean dataMode = false;
    private boolean attachmentMode = false;
    private boolean chunkMode = false;
    private StringBuilder mailContent = new StringBuilder();
    private List<String> recipients = new ArrayList<>();
    private String sender;

    @Value("${attachment.storage.dir}")
    private String storageDir;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private MailMapper mailMapper;

    @Autowired
    private AttachmentMapper attachmentMapper;

    @Autowired
    private AttachmentService attachmentService;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        System.out.println("SMTP Received: " + msg);
        String[] lines = msg.split("\r\n");
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
                    //收到的报文块处理：附件模式和块模式，防止附件数据块添加空行
                    if (command.contains("attachment")) {
                        //进入附件
                        attachmentMode = true;
                    } else if(command.contains("--boundary")) {
                        //离开附件
                        attachmentMode = false;
                    } else if (command.isEmpty() && attachmentMode && !chunkMode) {
                        //块开始的空行
                        chunkMode = true;
                        mailContent.append("\r\n");
                    } else if (command.isEmpty() && attachmentMode) {
                        //块结束的空行
                        chunkMode = false;
                        mailContent.append("\r\n");
                    }
                    if (attachmentMode && chunkMode) {
                        mailContent.append(command);
                    } else {
                        mailContent.append(command).append("\r\n");
                    }
                }
                ctx.writeAndFlush("250 OK: successfully getting content");
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
        System.out.println("---");
        System.out.println(mimeMessage);
        System.out.println("---");
        Properties props = new Properties();
        props.put("mail.smtp.ssl.trust", "false");
        Session session = Session.getInstance(props, null);
        MimeMessage message = new MimeMessage(session, new ByteArrayInputStream(mimeMessage.getBytes()));
        Mail mail = new Mail();

        Address[] from = message.getFrom();
        String senderEmail = from.length > 0 ? from[0].toString() : "unknown@flowmail.com";
        User sender = userMapper.findByEmail(senderEmail);
        mail.setSender_id(sender != null ? sender.getId() : null);
        System.out.println(senderEmail);

        Address[] to = message.getRecipients(Message.RecipientType.TO);
        String receiverEmail = to != null && to.length > 0 ? to[0].toString() : "unknown@flowmail.com";
        User receiver = userMapper.findByEmail(receiverEmail);
        mail.setReceiver_id(receiver != null ? receiver.getId() : null);
        System.out.println(receiverEmail);

        // 解决中文乱码
        String subject = new String(message.getSubject().getBytes("ISO8859_1"), StandardCharsets.UTF_8);
        mail.setSubject(subject);
        System.out.println(subject);

        Object content = message.getContent();
        List<String> attachmentNames = new ArrayList<>();
        List<byte[]> attachmentContents = new ArrayList<>();

        if (content instanceof Multipart) {
            Multipart multipart = (Multipart) content;
            StringBuilder contentBuilder = new StringBuilder();
            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart bodyPart = multipart.getBodyPart(i);
                String contentType = bodyPart.getContentType();

                if (contentType.contains("text/plain")) {
                    String textContent = ((String) bodyPart.getContent());
                    contentBuilder.append(textContent);
                    System.out.println(textContent);
                } /* else if (contentType.contains("text/html")) {
                    String htmlContent = (String) bodyPart.getContent();
                    contentBuilder.append(htmlContent);
                } */else if (contentType.contains("application/octet-stream") ||
                        contentType.contains("application/")) {
                    // 处理附件
                    try (InputStream is = bodyPart.getInputStream();
                         ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
                        byte[] data = new byte[76 * 10];
                        int nRead;
                        while ((nRead = is.read(data, 0, data.length)) != -1) {
                            buffer.write(data, 0, nRead);
                        }
                        buffer.flush();
                        attachmentNames.add(new String(bodyPart.getFileName().getBytes("ISO8859_1"), StandardCharsets.UTF_8));
                        attachmentContents.add(buffer.toByteArray());
                        System.out.println(attachmentNames.get(attachmentNames.size()-1));
                        System.out.println(new String(attachmentContents.get(attachmentContents.size()-1), StandardCharsets.UTF_8));
                    }
                }
            }
            mail.setContent(contentBuilder.toString());
            // 这里可以将附件信息保存到数据库或其他地方
            // 例如：mail.setAttachments(attachmentNames, attachmentContents);
        } else if (content instanceof String) {
            mail.setContent(content.toString());
        }
        //邮件信息设置
        mail.setCreate_at(LocalDateTime.now());
        mail.setSender_sign((short) 0);
        mail.setReceiver_sign((short) 0);
        mail.setRead((short) 0);
        mail.setSender_star((short) 0);
        mail.setReceiver_star((short) 0);
        if (!attachmentNames.isEmpty() && attachmentNames.size() == attachmentContents.size()) {
            mail.setWithAttachment((short) 1);
        } else {
            mail.setWithAttachment((short) 0);
        }
        mailMapper.insertMail(mail);
        if(!attachmentNames.isEmpty() && attachmentNames.size() == attachmentContents.size()) {
            long mail_id = mail.getMail_id();
            String fileName;
            String mimeType;
            File file;
            MimetypesFileTypeMap mimeTypesMap = new MimetypesFileTypeMap();
            for (int i = 0; i < attachmentNames.size(); i++) {
                fileName = attachmentNames.get(i);
                file = new File(fileName);
                mimeType = mimeTypesMap.getContentType(file);
                saveAttachment(mail_id, attachmentContents.get(i), fileName, mimeType);
            }
        }
        System.out.println("pass");
        // 如果需要保存附件信息，可以在这里调用相应的方法
    }


    /**
     * 保存附件到磁盘并记录到数据库
     * @param emailId    关联的邮件ID
     * @param fileBytes 文件字节流（由协议模块解析后传入）
     * @param fileName  原始文件名
     * @param fileType  文件类型
     */
    public Attachment saveAttachment(Long emailId, byte[] fileBytes, String fileName, String fileType) {
        //  生成唯一文件名和存储路径
        String uniqueFileName = UUID.randomUUID() + "_" + fileName;
        Path filePath = Paths.get(storageDir, uniqueFileName);

        // 2. 确保目录存在并写入文件
        try {
            Files.createDirectories(filePath.getParent());
            Files.write(filePath, fileBytes);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save attachment", e);
        }

        // 3. 保存记录到数据库
        Attachment attachment = new Attachment();
        attachment.setEmailId(emailId);
        attachment.setFileName(fileName);
        attachment.setFilePath(filePath.toString());
        attachment.setFileType(fileType);
        attachment.setFileSize((long) fileBytes.length);
        attachmentMapper.insert(attachment);

        return attachment;
    }

}