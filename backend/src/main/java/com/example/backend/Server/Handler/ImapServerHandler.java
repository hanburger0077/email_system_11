package com.example.backend.Server.Handler;

import com.example.backend.DTO.MailDTO;
import com.example.backend.entity.Attachment;
import com.example.backend.entity.Mail;
import com.example.backend.entity.User;
import com.example.backend.mapper.AttachmentMapper;
import com.example.backend.mapper.MailMapper;
import com.example.backend.mapper.UserMapper;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Component
@Scope("prototype")
public class ImapServerHandler extends SimpleChannelInboundHandler<String> {

    private final MailMapper mailMapper;
    private final UserMapper userMapper; // 假设有一个用户服务用于验证
    private final AttachmentMapper attachmentMapper;
    private boolean isAuthenticated = false;

    private long userId;
    private String mailbox;
    private short sender_sign = 0;
    private short receiver_sign = 0;
    private boolean isIdling = false;
    private LocalDateTime lastCheckTime = null;         // 上次检查时间
    private LocalDateTime lastHeartbeatTime = null;     // 新增：最后心跳时间
    private ScheduledFuture<?> idlePollingFuture;       // 轮询数据库任务
    private ScheduledFuture<?> heartbeatCheckFuture;    // 心跳检测定时任务

    private static final long POLL_INTERVAL = 5000;             // 轮询间隔（毫秒）
    private static final long HEARTBEAT_TIMEOUT = 60000;        // 心跳超时时间（毫秒）
    private static final long HEARTBEAT_CHECK_INTERVAL = 30000; // 心跳检测间隔（毫秒）

    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;


    public ImapServerHandler(MailMapper mailMapper, UserMapper userMapper, AttachmentMapper attachmentMapper) {
        this.mailMapper = mailMapper;
        this.userMapper = userMapper;
        this.attachmentMapper = attachmentMapper;
    }


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        System.out.println("IMAP Received: " + msg);

        // Trim and split the message to handle commands
        //String command = msg.trim().toUpperCase();
        String command = msg.trim();
        
        if (command.startsWith("LOGIN")) {
            //处理登录命令
            processLogin(ctx, command);
        } else if (isAuthenticated) {
            if (command.startsWith("SELECT")) {
                processSelect(ctx, command);
            } else if (command.startsWith("FETCH")) {
                processFetch(ctx, command);
            } else if (command.startsWith("SEARCH")) {
                processSearch(ctx, command);
            } else if (command.startsWith("STORE")) {
                processStore(ctx, command);
            } else if (command.startsWith("IDLE")) {
                processIdle(ctx);
            } else if (command.startsWith("DONE")) {
                processDone(ctx);
            } else if (command.startsWith("DELETE")) {
                processDelete(ctx, command);
            } else if (command.startsWith("DRAFT")) {
                processDraft(ctx, msg);
            } else if (command.startsWith("ATTACHMENT")) {
                processAttachment(ctx, command);
            } else if (command.startsWith("QUIT")) {
                processQuit(ctx);
            } else if (command.startsWith("NOOP")) {
                processNoop(ctx);
            } else {
                // 未知命令
                ctx.writeAndFlush("* BAD Command unrecognized\r\n");
            }
            lastHeartbeatTime = LocalDateTime.now();
        }
    }


    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        // 启动心跳检测任务
        //startHeartbeatCheck(ctx);
    }


    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        // 连接关闭时清理资源
        clearAndClose(ctx);
    }

/*
    private void startHeartbeatCheck(ChannelHandlerContext ctx) {
        // 取消已有的心跳检测任务
        cancelHeartbeatCheck();

        heartbeatCheckFuture = ctx.executor().scheduleAtFixedRate(() -> {
            if (isAuthenticated && lastHeartbeatTime != null) {
                long timeSinceLastHeartbeat = java.time.Duration.between(lastHeartbeatTime, LocalDateTime.now()).toMillis();
                System.out.println(timeSinceLastHeartbeat);
                if (timeSinceLastHeartbeat > HEARTBEAT_TIMEOUT) {
                    System.out.println("Heartbeat timeout, closing connection");
                    clearAndClose(ctx);
                }
            }
        }, HEARTBEAT_CHECK_INTERVAL, HEARTBEAT_CHECK_INTERVAL, TimeUnit.MILLISECONDS);
    }


    private void cancelHeartbeatCheck() {
        if (heartbeatCheckFuture != null) {
            heartbeatCheckFuture.cancel(false);
            heartbeatCheckFuture = null;
        }
    }
    */


    private void processNoop(ChannelHandlerContext ctx) {
        System.out.println("Hearing the heartbeat");
        // NOOP命令可以作为隐式心跳
        lastHeartbeatTime = LocalDateTime.now();
        ctx.writeAndFlush("1 OK NOOP completed\r\n");
    }


    private void processQuit(ChannelHandlerContext ctx) {
        // 处理QUIT命令，客户端主动关闭连接
        ctx.writeAndFlush("* BYE IMAP server closing connection\r\n");
        ctx.writeAndFlush("1 OK QUIT completed\r\n");
        clearAndClose(ctx);
    }


    private void processAttachment(ChannelHandlerContext ctx, String command) {
        String[] items = command.split(" ");
        Attachment attachment = attachmentMapper.selectById(Long.parseLong(items[1]));
        sendAttachmentResponse(ctx, attachment);
    }


    private void processDraft(ChannelHandlerContext ctx, String command) {
        String[] items = command.split("\r\n");
        Mail mail =new Mail();
        User sender = userMapper.findByEmail(items[1]);
        if (sender == null){
            ctx.writeAndFlush("* BAD mail saved failed\n");
            return;
        }
        User receiver = userMapper.findByEmail(items[2]);
        if (receiver == null){
            ctx.writeAndFlush("* BAD unknown receiver\n");
            return;
        }
        if (items[3] == null){
            ctx.writeAndFlush("* BAD no subject\n");
            return;
        }
        if (items[4] == null){
            ctx.writeAndFlush("* BAD no content\n");
            return;
        }
        long mailId = Long.parseLong(items[5]);
        mail.setMail_id(mailId);
        mail.setSender_id(sender.getId());
        mail.setReceiver_id(receiver.getId());
        mail.setSubject(items[3]);
        mail.setContent(items[4].replace("\n", "\r\n"));
        if (mailId == 0) {
            mail.setCreate_at(LocalDateTime.now());
            mail.setSender_sign((short) 1);
            mail.setReceiver_sign((short) 3);
            mail.setRead((short) 0);
            mail.setSender_star((short) 0);
            mail.setReceiver_star((short) 0);
            mailMapper.insertMail(mail);
            ctx.writeAndFlush("1 OK draft update complete\r\n");
        } else {
            if (mailMapper.selectByMailId(mailId) != null) {
                mailMapper.draftChange(mail);
                ctx.writeAndFlush("1 OK draft update complete\r\n");
            }
        }
    }


    private void processDelete(ChannelHandlerContext ctx, String command) {
        //删除邮件
        long mailId = Long.parseLong(command.split(" ")[1]);
        Mail mail = mailMapper.selectByMailId(mailId);
        if (mail.getSender_id() == userId) {
            // 3为接收方标签的逻辑删除标签
            if (mail.getReceiver_sign() == 3) {
                if (mail.getWithAttachment() == 1) {
                    List<Long> attachmentIds = attachmentMapper.selectByEmailId(mailId);
                    for(Long attachmentId : attachmentIds) {
                        deleteAttachment(attachmentId);
                    }
                    attachmentMapper.deleteByEmailId(mailId);
                }
                mailMapper.deleteMail(mailId);
            } else {
                mailMapper.setDeleteSign(mailId, "sender");
            }
        }
        else if (mail.getReceiver_id() == userId) {
            // 2为发送方标签的逻辑删除标签
            if (mail.getSender_sign() == 2) {
                System.out.println(mail.getWithAttachment());
                if (mail.getWithAttachment() == 1) {
                    List<Long> attachmentIds = attachmentMapper.selectByEmailId(mailId);
                    for(Long attachmentId : attachmentIds) {
                        deleteAttachment(attachmentId);
                    }
                    attachmentMapper.deleteByEmailId(mailId);
                }
                mailMapper.deleteMail(mailId);
            } else {
                mailMapper.setDeleteSign(mailId, "receiver");
            }
        }
        ctx.writeAndFlush("1 OK DELETE completed\r\n");
    }


    private void processDone(ChannelHandlerContext ctx) {
        // 处理DONE命令，客户端有东西要发，别推送
        if (isIdling) {
            isIdling = false;
            ctx.writeAndFlush("1 OK DONE start\r\n");
        } else {
            ctx.writeAndFlush("* BAD Not in IDLE state\r\n");
        }
    }


    private void processIdle(ChannelHandlerContext ctx) {
        // 处理IDLE命令，客户端发完了，可以推送
        isIdling = true;
        ctx.writeAndFlush("+ idling\r\n");
        // 开始后台轮询检查新邮件
        startIdlePolling(ctx);
    }

    
    private void processStore(ChannelHandlerContext ctx, String command) {
        String[] items = command.split(" ");
        if (items.length == 4) {
            long mailId = Long.parseLong(items[1]);
            String op = items[2];
            String sign = items[3];
            mailMapper.changeState(mailId, sign, op);
            System.out.println(sign);
            ctx.writeAndFlush("1 OK STORE completed\r\n");
        } else {
            ctx.writeAndFlush("* BAD STORE command error\r\n");
        }
    }

    
    private void processSearch(ChannelHandlerContext ctx, String command) {
        List<Long> mailId;
        String[] items = command.substring(7).split(" ");
        if (items.length == 1 && Objects.equals(items[0], "ALL")) {
            if (mailbox.equals("INBOX") || mailbox.equals("TRASH") || mailbox.equals("JUNK")) {
                mailId = mailMapper.selectByReceiverIdWithSign(userId, 0, null, null, null, receiver_sign, null, null);
            } else if (mailbox.equals("SENT") || mailbox.equals("DRAFT")) {
                mailId = mailMapper.selectBySenderIdWithSign(userId, 0, null, null, null, sender_sign, null);
            } else {
                ctx.writeAndFlush("* BAD SEARCH failed because mailbox does not existing\r\n");
                return;
            }
        } else {
            String from = null;
            long senderId = 0;
            String to = null;
            long receiverId = 0;
            String subject = null;
            String body = null;
            LocalDateTime since = null;
            String read = null;
            String sender_star = null;
            String receiver_star = null;
            for (String item : items) {
                if (item.startsWith("FROM:")) {
                    from = item.substring(5);
                    User sender = userMapper.findByEmail(from);
                    if (sender != null) senderId = sender.getId();
                }
                if (item.startsWith("TO:")) {
                    to = item.substring(3);
                    User receiver = userMapper.findByEmail(to);
                    if (receiver != null) receiverId = receiver.getId();
                }
                if (item.startsWith("SUBJECT:")) {
                    subject = item.substring(8);
                }
                if (item.startsWith("BODY:")) {
                    body = item.substring(5);
                }
                if (item.startsWith("SINCE:")) {
                    since = LocalDateTime.parse(item.substring(6));
                }
                if (item.equals("UNSEEN") || item.equals("SEEN")) {
                    read = item;
                }
                if (item.equals("S_STAR")) {
                    sender_star = item;
                }
                if (item.equals("R_STAR")) {
                    receiver_star = item;
                }
            }
            if (mailbox.equals("INBOX") || mailbox.equals("TRASH") || mailbox.equals("JUNK")) {
                mailId = mailMapper.selectByReceiverIdWithSign(userId, senderId, subject, body, since, receiver_sign, read, receiver_star);
            } else if (mailbox.equals("SENT") || mailbox.equals("DRAFT")) {
                mailId = mailMapper.selectBySenderIdWithSign(userId, receiverId, subject, body, since, sender_sign, sender_star);
            } else {
                ctx.writeAndFlush("* BAD SEARCH failed because mailbox does not existing\r\n");
                return;
            }
        }
        StringBuffer response = new StringBuffer("* SEARCH");
        for (long id : mailId) {
            response.append(" ").append(id);
        }
        response.append("\r\n");
        ctx.writeAndFlush(response.toString());
        ctx.writeAndFlush("1 OK SEARCH completed\r\n");
    }

    
    private void processFetch(ChannelHandlerContext ctx, String command) {
        // 根据邮件id查找邮件具体内容 (命令格式 "FETCH <mailId>")
        String[] parts = command.split(" ");
        if (parts.length == 3) {
            if (Objects.equals(parts[1], "DETAIL")) {
                long mailId = Long.parseLong(parts[2]);
                mailMapper.changeState(mailId, "READ", "+FLAG");
                Mail mail = mailMapper.selectByMailId(mailId);
                if (mail != null) {
                    List<Long> attachmentIds = attachmentMapper.selectByEmailId(mailId);
                    sendMailDetailResponse(ctx, mail, attachmentIds);
                    ctx.writeAndFlush("1 OK FETCH completed\r\n");
                } else {
                    ctx.writeAndFlush("* BAD Mail not found\r\n");
                }
            } else if (Objects.equals(parts[1], "SIMPLE")) {
                long mailId = Long.parseLong(parts[2]);
                Mail mail = mailMapper.selectByMailId(mailId);
                if (mail != null) {
                    sendMailSimpleResponse(ctx, mail);
                    ctx.writeAndFlush("1 OK FETCH completed\r\n");
                } else {
                    ctx.writeAndFlush("* BAD Mail not found\r\n");
                }
            }
        } else {
            ctx.writeAndFlush("* BAD Invalid FETCH command\r\n");
        }
    }

    
    private void processSelect(ChannelHandlerContext ctx, String command) {
        // 选择收件箱命令，INBOX/SENT/DRAFT/TRASH/JUNK
        // 实际先区分为接收方还是发送方
        // 接收方有 INBOX:0/TRASH:1/JUNK:2
        // 发送方有 SENT:0/DRAFT:1
        // 提取文件夹名称
        mailbox = command.split(" ")[1].toUpperCase();
        int count;
        // 根据文件夹名称获取邮件数据
        switch (mailbox) {
            // 接收方为自身，选择接收方标签为0的邮件
            case "INBOX":
                receiver_sign = 0;
                count = mailMapper.countByReceiverIdWithSign(userId, receiver_sign);
                break;
            // 发送方为自身，选择发送方标签为0的邮件
            case "SENT":
                sender_sign = 0;
                System.out.println(userId);
                count = mailMapper.countBySenderIdWithSign(userId, sender_sign);
                break;
            // 发送方为自身，发选择送方标签为1的邮件
            case "DRAFT":
                sender_sign = 1;
                count = mailMapper.countBySenderIdWithSign(userId, sender_sign);
                break;
            // 接收方为自身，选择接收方标签为1的邮件
            case "TRASH":
                receiver_sign = 1;
                count = mailMapper.countByReceiverIdWithSign(userId, receiver_sign);
                break;
            // 接收方为自身，选择接收方标签为2的邮件
            case "JUNK":
                receiver_sign = 2;
                count = mailMapper.countByReceiverIdWithSign(userId, receiver_sign);
                break;
            // 处理其他文件夹...
            default:
                ctx.writeAndFlush("* BAD [TRYCREATE] Folder doesn't exist\r\n");
                return;
        }
        ctx.writeAndFlush("* " + count + " EXISTS\r\n");
        ctx.writeAndFlush("1 OK [READ-WRITE] SELECT completed\r\n");
    }

    
    private void processLogin(ChannelHandlerContext ctx, String command) {
        String[] parts = command.split(" ");
        if (parts.length == 3) {
            String userEmail = parts[1];
            String password = parts[2];
            User user = userMapper.findByEmail(userEmail);
            if (user != null && bCryptPasswordEncoder.matches(password, user.getPassword())) {
                //登陆成功后授权和自动进入空闲
                isAuthenticated = true;
                isIdling = true;
                userId = user.getId();
                startIdlePolling(ctx);
                lastCheckTime = LocalDateTime.now();
                ctx.writeAndFlush("1 OK LOGIN completed\r\n");
            } else {
                ctx.writeAndFlush("* BAD Authentication failed\r\n");
                ctx.close();
            }
        } else {
            ctx.writeAndFlush("* BAD Invalid LOGIN command\r\n");
            ctx.close();
        }
    }


    private void startIdlePolling(ChannelHandlerContext ctx) {
        cancelIdlePolling();

        idlePollingFuture = ctx.executor().scheduleAtFixedRate(() -> {
            try {
                System.out.println("polling: isIdling=" + isIdling + ", isAuthenticated=" + isAuthenticated);
                if (isIdling && isAuthenticated) {
                    checkForNewMails(ctx);
                }
            } catch (Exception e) {
                System.err.println("Idle polling error: " + e.getMessage());
                // 可以选择在这里重新启动轮询或记录错误
            }
        }, POLL_INTERVAL, POLL_INTERVAL, TimeUnit.MILLISECONDS);
    }


    private void checkForNewMails(ChannelHandlerContext ctx) {
        int newMailNum = mailMapper.checkNewMail(userId, lastCheckTime);
        if (newMailNum > 0) {
            ctx.writeAndFlush("* RECENT " + newMailNum);
        }
        lastCheckTime = LocalDateTime.now();
        // 可以添加保存用户的状态，方便下一次登录时立马推送未登录期间获取的新邮件
    }


    private void cancelIdlePolling() {
        if (idlePollingFuture != null) {
            idlePollingFuture.cancel(false);
            idlePollingFuture = null;
        }
    }


    public void clearAndClose(ChannelHandlerContext ctx) {
        // 取消轮询任务
        cancelIdlePolling();
        //cancelHeartbeatCheck();
        // 关闭连接
        ctx.close();
    }

    
    // 辅助方法：发送完整的邮件响应
    private void sendMailDetailResponse(ChannelHandlerContext ctx, Mail mail, List<Long> attachmentIds) {
        MailDTO mailDTO = new MailDTO(mail, userMapper, attachmentIds);
        String response = String.format("* " + mail.getMail_id() + " FETCH (DATA[] {%d}\r\n%s\r\n)",
                mailDTO.getMailDetailLength(),
                mailDTO.getMailDetail());
        ctx.writeAndFlush(response);
    }

    private void sendMailSimpleResponse(ChannelHandlerContext ctx, Mail mail) {
        MailDTO mailDTO = new MailDTO(mail, userMapper, null);
        String response = String.format("* " + mail.getMail_id() + " FETCH (DATA[] {%d}\r\n%s\r\n)\r\n",
                mailDTO.getMailSimpleLength(),
                mailDTO.getMailSimple());
        ctx.writeAndFlush(response);
    }


    private void sendAttachmentResponse(ChannelHandlerContext ctx, Attachment attachment) {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("ID ").append(attachment.getId()).append("\r\n");
        stringBuffer.append("MAILID ").append(attachment.getEmailId()).append("\r\n");
        stringBuffer.append("NAME ").append(attachment.getFileName()).append("\r\n");
        stringBuffer.append("TYPE ").append(attachment.getFileType()).append("\r\n");
        stringBuffer.append("SIZE ").append(attachment.getFileSize()).append("\r\n");
        stringBuffer.append("PATH ").append(attachment.getFilePath()).append("\r\n");
        ctx.writeAndFlush(stringBuffer.toString());
        ctx.writeAndFlush("1 OK Attachment information fetch successfully\r\n");
    }


    private void deleteAttachment(Long attachmentId) {
        // 1. 从数据库查询附件信息
        Attachment attachment = attachmentMapper.selectById(attachmentId);
        if (attachment == null) {
            return; // 附件不存在
        }
        // 2. 删除文件系统中的文件
        Path filePath = Paths.get(attachment.getFilePath());
        try {
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

}