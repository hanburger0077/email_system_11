package com.example.backend.Server.Handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class ImapServerHandler extends SimpleChannelInboundHandler<String> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        System.out.println("IMAP Received: " + msg);
        // Process IMAP commands here
        if (msg.startsWith("LOGIN")) {
            // Handle login command
            ctx.writeAndFlush("* OK [CAPABILITY IMAP4rev1] Login successful\r\n");
        } else if (msg.startsWith("SELECT")) {
            // Handle select command
            ctx.writeAndFlush("* 1 EXISTS\r\n");
        } else if (msg.startsWith("FETCH")) {
            // Handle fetch command
            ctx.writeAndFlush("* 1 FETCH (BODY[] {3}\r\nHi)\r\n");
        } else if (msg.startsWith("QUIT")) {
            ctx.writeAndFlush("* BYE IMAP server closing connection\r\n");
            ctx.close();
        } else {
            ctx.writeAndFlush("* BAD Command unrecognized\r\n");
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}