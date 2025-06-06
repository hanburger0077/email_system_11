package com.example.backend.Client;

import com.example.backend.Client.Handler.SmtpClientHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.CharsetUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Base64;


@Component
public class SmtpClient {

    private final String host;
    private final int port;
    private Channel channel;
    private EventLoopGroup group;

    public SmtpClient(
            @Value("${server.smtp.host}") String host,
            @Value("${server.smtp.port}") int port) {
        this.host = host;
        this.port = port;
    }


    public void connect() throws InterruptedException {
        group = new NioEventLoopGroup();
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new StringDecoder(CharsetUtil.UTF_8));
                        pipeline.addLast(new StringEncoder(CharsetUtil.UTF_8));
                        pipeline.addLast(new SmtpClientHandler());
                    }
                });

        ChannelFuture future = bootstrap.connect(host, port).sync();
        channel = future.channel();
    }


    //发送纯文本文件
    public void sendSimpleMail(String from, String to, String subject, String body) throws InterruptedException {
        if (channel != null && channel.isActive()) {
            SmtpClientHandler handler = channel.pipeline().get(SmtpClientHandler.class);
            String response;
            // 发送 HELO 命令并等待响应
            response = handler.sendCommand(channel, "HELO localhost");
            System.out.println("SMTP return：" + response);

            // 发送 MAIL FROM 命令并等待响应
            handler.sendCommand(channel, "MAIL FROM:<" + from + ">");
            System.out.println("SMTP return：" + response);

            // 发送 RCPT TO 命令并等待响应
            handler.sendCommand(channel, "RCPT TO:<" + to + ">");
            System.out.println("SMTP return：" + response);

            // 发送 DATA 命令并等待响应
            handler.sendCommand(channel, "DATA");
            System.out.println("SMTP return：" + response);

            // 构建邮件内容
            String emailMessage = "From: " + from + "\r\n" +
                    "To: " + to + "\r\n" +
                    "Subject: " + subject + "\r\n" +
                    "MIME-Version: 1.0\r\n" +
                    "Content-Type: text/plain; charset=UTF-8\r\n\r\n" +
                    body + "\r\n" +
                    ".\r\n";

            // 发送邮件内容
            channel.writeAndFlush(emailMessage).addListener(future -> {
                if (!future.isSuccess()) {
                    future.cause().printStackTrace();
                }
            });

            System.out.println("Sending mail successfully");
        }
    }


    //发送带附件邮件
    public void sendMail(String from, String to, String subject, String textBody, String htmlBody, String attachmentName, byte[] attachmentContent) throws Exception {
        if (channel != null && channel.isActive()) {
            SmtpClientHandler handler = channel.pipeline().get(SmtpClientHandler.class);
            String response;
            // 发送 HELO 命令并等待响应
            response = handler.sendCommand(channel, "HELO localhost");
            System.out.println("SMTP return：" + response);

            // 发送 MAIL FROM 命令并等待响应
            handler.sendCommand(channel, "MAIL FROM:<" + from + ">");
            System.out.println("SMTP return：" + response);

            // 发送 RCPT TO 命令并等待响应
            handler.sendCommand(channel, "RCPT TO:<" + to + ">");
            System.out.println("SMTP return：" + response);

            // 发送 DATA 命令并等待响应
            handler.sendCommand(channel, "DATA");
            System.out.println("SMTP return：" + response);

            // 构建 MIME 消息
            String boundary = "==MultipartBoundary" + System.currentTimeMillis();
            String mimeMessage = "From: " + from + "\r\n" +
                    "To: " + to + "\r\n" +
                    "Subject: " + subject + "\r\n" +
                    "MIME-Version: 1.0\r\n" +
                    "Content-Type: multipart/mixed; boundary=\"" + boundary + "\"\r\n\r\n" +
                    "--" + boundary + "\r\n" +
                    "Content-Type: multipart/alternative; boundary=\"alt=" + boundary + "\"\r\n\r\n" +
                    "--" + boundary + "alt\r\n" +
                    "Content-Type: text/plain; charset=UTF-8\r\n\r\n" +
                    textBody + "\r\n\r\n" +
                    "--" + boundary + "alt\r\n" +
                    "Content-Type: text/html; charset=UTF-8\r\n\r\n" +
                    htmlBody + "\r\n\r\n" +
                    "--" + boundary + "alt--\r\n";

            // 添加附件（如果提供）
            if (attachmentName != null && attachmentContent != null) {
                mimeMessage += "--" + boundary + "\r\n" +
                        "Content-Type: application/octet-stream; name=\"" + attachmentName + "\"\r\n" +
                        "Content-Disposition: attachment; filename=\"" + attachmentName + "\"\r\n" +
                        "Content-Transfer-Encoding: base64\r\n\r\n" +
                        Base64.getEncoder().encodeToString(attachmentContent) + "\r\n";
            }

            mimeMessage += "--" + boundary + "--\r\n" +
                    ".\r\n";

            // 发送邮件内容
            channel.writeAndFlush(mimeMessage).addListener(future -> {
                if (!future.isSuccess()) {
                    future.cause().printStackTrace();
                }
            });
        }
    }

    public void disconnect() {
        if (channel != null) {
            channel.close();
        }
        if (group != null) {
            group.shutdownGracefully();
        }
    }
}



