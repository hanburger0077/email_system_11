package com.example.backend.Client.Handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.Setter;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;


public class ImapClientHandler extends SimpleChannelInboundHandler<String> {
    private CountDownLatch latch;
    private StringBuilder responseBuffer = new StringBuilder();
    private String completeResponse;
    private boolean isIdling = false;
    private static final long IDLE_TIMEOUT_SECONDS = 4 * 60 + 50;
    private static final long HEARTBEAT_SECONDS = 4 * 60;

    // 添加回调接口
    @Setter
    private Consumer<String> eventCallback;


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        responseBuffer.append(msg);
        // 判断条件为 contains 而不是 startswith，防止消息粘滞导致无法唤醒客户端线程
        if (msg.contains("* BAD") || msg.contains("1 OK") || msg.contains("+ idling") || msg.contains("DONE") || msg.startsWith("* RECENT")) {
            System.out.println(msg);
            completeResponse = responseBuffer.toString();
            responseBuffer.setLength(0); // 清空缓存
            if (latch != null) {
                latch.countDown(); // 通知线程
            }
            if (msg.contains("DONE")){
                isIdling = false;
                handleServerEvent(msg);
            }
            // 检查是否有新邮件通知
            if (msg.contains("* RECENT")) {
                if (eventCallback != null) {
                    eventCallback.accept(msg);
                }
            }
        }

    }


    public String sendCommand(Channel channel, String command) throws InterruptedException {
        latch = new CountDownLatch(1);
        channel.writeAndFlush(command + "\r\n");
        latch.await(10, TimeUnit.SECONDS); // 设置超时，防止无限期等待响应
        return completeResponse;
    }


    public String idle(Channel channel) throws InterruptedException {
        isIdling = true;
        latch = new CountDownLatch(1);
        channel.writeAndFlush("IDLE\r\n");
        if (!latch.await(IDLE_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            // 超时未收到 DONE 命令，主动退出空闲状态
            System.out.println("IDLE timeout, sending DONE...");
            done(channel);
            return "IDLE timeout";
        }
        return completeResponse;
    }


    public void done(Channel channel) throws InterruptedException {
        if (isIdling) {
            channel.writeAndFlush("DONE\r\n");
            isIdling = false;
        }
    }

    //心跳机制，防止超时断开
    public void sendNoop(Channel channel) throws InterruptedException {
        latch = new CountDownLatch(1);
        channel.writeAndFlush("NOOP\r\n");
        latch.await(10, TimeUnit.SECONDS); // 设置超时时间
    }

    //处理新邮件到达
    private void handleServerEvent(String event) {
        System.out.println("Server event: " + event);
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}