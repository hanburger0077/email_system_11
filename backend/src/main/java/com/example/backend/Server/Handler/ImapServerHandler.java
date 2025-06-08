package com.example.backend.Server.Handler;

import com.example.backend.DTO.MailDTO;
import com.example.backend.entity.Mail;
import com.example.backend.entity.User;
import com.example.backend.mapper.MailMapper;
import com.example.backend.mapper.UserMapper;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Component
@Scope("prototype")
public class ImapServerHandler extends SimpleChannelInboundHandler<String> {

    private final MailMapper mailMapper;
    private final UserMapper userMapper; // 假设有一个用户服务用于验证
    private boolean isAuthenticated = false;

    private long userId;
    private String mailbox;
    private short sender_sign;
    private short receiver_sign;

    public ImapServerHandler(MailMapper mailMapper, UserMapper userMapper) {
        this.mailMapper = mailMapper;
        this.userMapper = userMapper;
        this.sender_sign = 0;
        this.receiver_sign = 0;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        System.out.println("IMAP Received: " + msg);

        // Trim and split the message to handle commands
        String command = msg.trim().toUpperCase();


        if (command.startsWith("LOGIN")) {
            //登录命令
            String[] parts = command.split(" ");
            if (parts.length == 3) {
                String userEmail = parts[1];
                String password = parts[2];
                User user = userMapper.findByEmail(userEmail);
                if (user != null && userMapper.authenticate(userEmail, password)) {
                    isAuthenticated = true;
                    userId = user.getId();
                    ctx.writeAndFlush("1 OK LOGIN completed\r\n");
                } else {
                    ctx.writeAndFlush("* BAD Authentication failed\r\n");
                }
            } else {
                ctx.writeAndFlush("* BAD Invalid LOGIN command\r\n");
            }
        } else if (command.startsWith("SELECT")) {
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
            ctx.writeAndFlush("* " + count +" EXISTS\r\n");
            ctx.writeAndFlush("1 OK [READ-WRITE] SELECT completed\r\n");
        } else if (command.startsWith("FETCH")) {
            // 根据邮件id查找邮件具体内容 (命令格式 "FETCH <mailId>")
            String[] parts = command.split(" ");
            if (parts.length == 3) {
                if(Objects.equals(parts[1], "DETAIL")) {
                    long mailId = Long.parseLong(parts[2]);
                    Mail mail = mailMapper.selectByMailId(mailId);
                    if (mail != null) {
                        sendMailDetailResponse(ctx, mail);
                        ctx.writeAndFlush("1 OK FETCH completed\r\n");
                    } else {
                        ctx.writeAndFlush("* BAD Mail not found\r\n");
                    }
                }
                else if(Objects.equals(parts[1], "SIMPLE")) {
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
        } else if (command.startsWith("SEARCH")) {
            List<Long> mailId;
            String[] items = command.substring(7).split(" ");
            if (items.length == 1 && Objects.equals(items[0], "ALL")) {
                if (mailbox.equals("INBOX") || mailbox.equals("TRASH") || mailbox.equals("JUNK")) {
                    mailId = mailMapper.selectByReceiverIdWithSign(userId, 0, null, null, null, receiver_sign, null, null);
                }
                else if (mailbox.equals("SENT") || mailbox.equals("DRAFT")) {
                    mailId = mailMapper.selectBySenderIdWithSign(userId, 0, null, null, null, sender_sign, null);
                }
                else {
                    ctx.writeAndFlush("* BAD SEARCH failed because mailbox does not existing\r\n");
                    return;
                }
            }
            else {
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
                        if(sender != null) senderId = sender.getId();
                    }
                    if (item.startsWith("TO:")) {
                        to = item.substring(3);
                        User receiver = userMapper.findByEmail(to);
                        if(receiver != null) receiverId = receiver.getId();
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
                    if (item.equals("UNSEEN") || item.equals("SEEN")){
                        read = item;
                    }
                    if (item.equals("S_STAR")){
                        sender_star = item;
                    }
                    if (item.equals("R_STAR")){
                        receiver_star = item;
                    }
                }
                if (mailbox.equals("INBOX") || mailbox.equals("TRASH") || mailbox.equals("JUNK")) {
                    mailId = mailMapper.selectByReceiverIdWithSign(userId, senderId, subject, body, since, receiver_sign, read, receiver_star);
                }
                else if (mailbox.equals("SENT") || mailbox.equals("DRAFT")) {
                    mailId = mailMapper.selectBySenderIdWithSign(userId, receiverId, subject, body, since, sender_sign, sender_star);
                }
                else {
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
        } else if (command.startsWith("IDLE")) {
            // Handle idle command
            // IDLE is a special command that waits for server notifications
            ctx.writeAndFlush("+ idling\r\n");
        } else if (command.startsWith("DONE")) {
            // Handle done command
            // Exit IDLE mode
            ctx.writeAndFlush("1 OK IDLE finished\r\n");
        } else if (command.startsWith("QUIT")) {
            // Handle quit command
            ctx.writeAndFlush("* BYE IMAP server closing connection\r\n");
            ctx.writeAndFlush("1 OK QUIT completed\r\n");
            ctx.close();
        } else {
            // Handle unrecognized commands
            ctx.writeAndFlush("* BAD Command unrecognized\r\n");
        }
    }

    // 辅助方法：发送完整的邮件响应
    private void sendMailDetailResponse(ChannelHandlerContext ctx, Mail mail) {
        MailDTO mailDTO = new MailDTO(mail, userMapper);
        String response = String.format("* " + mail.getMail_id() + " FETCH (DATA[] {%d}\r\n%s\r\n)",
                mailDTO.getMailDetailLength(),
                mailDTO.getMailDetail());
        ctx.writeAndFlush(response);
    }

    private void sendMailSimpleResponse(ChannelHandlerContext ctx, Mail mail) {
        MailDTO mailDTO = new MailDTO(mail, userMapper);
        String response = String.format("* " + mail.getMail_id() + " FETCH (DATA[] {%d}\r\n%s\r\n)\r\n",
                mailDTO.getMailSimpleLength(),
                mailDTO.getMailSimple());
        ctx.writeAndFlush(response);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}