package com.example.backend.Server.Handler;

import com.example.backend.DTO.MailDTO;
import com.example.backend.entity.Mail;
import com.example.backend.mapper.MailMapper;
import com.example.backend.mapper.UserMapper;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class ImapServerHandler extends SimpleChannelInboundHandler<String> {

    private final MailMapper mailMapper;
    private final UserMapper userMapper; // 假设有一个用户服务用于验证
    private boolean isAuthenticated = false;

    public ImapServerHandler(MailMapper mailMapper, UserMapper userMapper) {
        this.mailMapper = mailMapper;
        this.userMapper = userMapper;
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
                String username = parts[1];
                String password = parts[2];
                if (userMapper.authenticate(username, password)) {
                    isAuthenticated = true;
                    ctx.writeAndFlush("1 OK LOGIN completed\r\n");
                } else {
                    ctx.writeAndFlush("* BAD Authentication failed\r\n");
                }
            } else {
                ctx.writeAndFlush("* BAD Invalid LOGIN command\r\n");
            }
        } else if (command.startsWith("SELECT")) {
            //选择命令
            // Handle select command
            // Select the inbox mailbox
            ctx.writeAndFlush("* 1 EXISTS\r\n");
            ctx.writeAndFlush("* 1 RECENT\r\n");
            ctx.writeAndFlush("* OK [UNSEEN 0] No unseen messages\r\n");
            ctx.writeAndFlush("1 OK [READ-WRITE] SELECT completed\r\n");
        } else if (command.startsWith("FETCH")) {
            // 根据邮件id查找邮件 (命令格式 "FETCH <mailId>")
            String[] parts = command.split(" ");
            if (parts.length > 1) {
                long mailId = Long.parseLong(parts[1]);
                Mail mail = mailMapper.selectByMailId(mailId);
                if (mail != null) {
                    sendMailResponse(ctx, mail);
                    ctx.writeAndFlush("1 OK FETCH completed\r\n");
                } else {
                    ctx.writeAndFlush("* BAD Mail not found\r\n");
                }
            } else {
                ctx.writeAndFlush("* BAD Invalid FETCH command\r\n");
            }
        } else if (command.startsWith("SEARCH")) {
            // Handle search command
            // Example: search for unseen messages
            ctx.writeAndFlush("* SEARCH\r\n");
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
    private void sendMailResponse(ChannelHandlerContext ctx, Mail mail) {
        MailDTO mailDTO = new MailDTO(mail, userMapper);
        String response = String.format("* " + mail.getMail_id() + " FETCH (BODY[] {%d}\r\n%s\r\n)",
                mailDTO.getMailStringLength(),
                mailDTO.getMailString());
        ctx.writeAndFlush(response);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}