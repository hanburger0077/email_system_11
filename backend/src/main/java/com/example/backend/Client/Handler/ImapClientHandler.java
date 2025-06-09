package com.example.backend.Client.Handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.concurrent.CountDownLatch;


public class ImapClientHandler extends SimpleChannelInboundHandler<String> {
    private CountDownLatch latch;
    private StringBuilder responseBuffer = new StringBuilder();
    private String completeResponse;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        responseBuffer.append(msg);
        // 判断条件为 contains 而不是 startswith，防止消息粘滞导致无法唤醒客户端线程
        if (msg.contains("* BAD") || msg.contains("1 OK")) {
            System.out.println(msg);
            completeResponse = responseBuffer.toString();
            responseBuffer.setLength(0); // 清空缓存
            if (latch != null) {
                latch.countDown(); // 通知线程
            }
        }
    }

    public String sendCommand(Channel channel, String command) throws InterruptedException {
        latch = new CountDownLatch(1);
        channel.writeAndFlush(command + "\r\n");
        latch.await(); // 等待相应
        return completeResponse;
    }
}