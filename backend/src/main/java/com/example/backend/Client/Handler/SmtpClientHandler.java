package com.example.backend.Client.Handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.concurrent.CountDownLatch;

public class SmtpClientHandler extends SimpleChannelInboundHandler<String> {
    private CountDownLatch latch;
    private String lastResponse;

    // 异步需要确保在发送每条命令后等待服务器的响应，然后再发送下一条命令。这种处理方式可以避免消息粘连问题
    // SMTP服务器响应每次都是一行命令
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        lastResponse = msg;
        //异步处理
        if (latch != null) {
            latch.countDown(); // 通知等待的线程
        }
    }

    public String sendMessage(Channel channel, String msg) throws InterruptedException {
        latch = new CountDownLatch(1);
        channel.writeAndFlush(msg);
        //异步处理
        latch.await(); // 等待响应
        return lastResponse;
    }
}
