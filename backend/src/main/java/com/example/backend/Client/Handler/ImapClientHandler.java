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
        responseBuffer.append(msg).append("\r\n");
        // 检查是否完整
        if (msg.startsWith("* BAD") || msg.startsWith("1 OK")) {
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