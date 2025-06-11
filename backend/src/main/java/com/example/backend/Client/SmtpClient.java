package com.example.backend.Client;

import com.example.backend.Client.Handler.SmtpClientHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.CharsetUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;


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
            response = handler.sendMessage(channel, "HELO localhost");
            System.out.println("SMTP return：" + response);

            // 发送 MAIL FROM 命令并等待响应
            handler.sendMessage(channel, "MAIL FROM:<" + from + ">");
            System.out.println("SMTP return：" + response);

            // 发送 RCPT TO 命令并等待响应
            handler.sendMessage(channel, "RCPT TO:<" + to + ">");
            System.out.println("SMTP return：" + response);

            // 发送 DATA 命令并等待响应
            handler.sendMessage(channel, "DATA");
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


    public void sendMail(String from, String to, String subject, String textBody, String htmlBody,
                         List<String> attachmentNames, List<byte[]> attachmentContents) throws Exception {
        if (channel != null && channel.isActive()) {
            SmtpClientHandler handler = channel.pipeline().get(SmtpClientHandler.class);
            String response;

            // 发送 HELO 命令并等待响应
            response = handler.sendMessage(channel, "HELO localhost" + "\r\n");
            System.out.println("SMTP return (HELO): " + response);

            // 发送 MAIL FROM 命令并等待响应
            response = handler.sendMessage(channel, "MAIL FROM:<" + from + ">" + "\r\n");
            System.out.println("SMTP return (MAIL FROM): " + response);

            // 发送 RCPT TO 命令并等待响应
            response = handler.sendMessage(channel, "RCPT TO:<" + to + ">" + "\r\n");
            System.out.println("SMTP return (RCPT TO): " + response);

            // 发送 DATA 命令并等待响应
            response = handler.sendMessage(channel, "DATA" + "\r\n");
            System.out.println("SMTP return (DATA): " + response);

            // 使用当前时间的毫秒数作为边界字符串的一部分
            long currentTimeMillis = System.currentTimeMillis();
            String boundary = "boundary" + currentTimeMillis;
            String altBoundary = "altBoundary" + currentTimeMillis;

            // 构建邮件头
            StringBuilder header = new StringBuilder();
            header.append("From: ").append(from).append("\r\n")
                    .append("To: ").append(to).append("\r\n")
                    .append("Subject: ").append(subject).append("\r\n")
                    .append("MIME-Version: 1.0\r\n")
                    .append("Content-Type: multipart/mixed; boundary=\"").append(boundary).append("\"\r\n\r\n");

            // 发送邮件头
            response = handler.sendMessage(channel, header.toString());
            System.out.println(response);

            // 构建文本和HTML部分
            StringBuilder textHtmlPart = new StringBuilder();
            textHtmlPart.append("--" + boundary + "\r\n")
                    .append("Content-Type: multipart/alternative; boundary=\"").append(altBoundary).append("\"\r\n\r\n")
                    .append("--" + altBoundary + "\r\n")
                    .append("Content-Type: text/plain; charset=UTF-8\r\n\r\n")
                    .append(textBody).append("\r\n\r\n")
                    .append("--" + altBoundary + "\r\n")
                    .append("Content-Type: text/html; charset=UTF-8\r\n\r\n")
                    .append(htmlBody).append("\r\n\r\n")
                    .append("--" + altBoundary + "--\r\n");

            // 发送文本和HTML部分
            response = handler.sendMessage(channel, textHtmlPart.toString());

            // 发送附件数据块
            for (int i = 0; i < attachmentNames.size(); i++) {
                String attachmentName = attachmentNames.get(i);
                byte[] attachmentContent = attachmentContents.get(i);
                StringBuilder attachmentHead = new StringBuilder();
                attachmentHead.append("--" + boundary + "\r\n")
                        .append("Content-Type: application/octet-stream; name=\"" + attachmentName + "\"\r\n")
                        .append("Content-Disposition: attachment; filename=\"" + attachmentName + "\"\r\n")
                        .append("Content-Transfer-Encoding: base64\r\n\r\n");

                // 发送附件头
                response = handler.sendMessage(channel, attachmentHead.toString());
                System.out.println(response);

                // 发送附件数据
                String attachmentData = Base64.getEncoder().encodeToString(attachmentContent);
                int chunkSize = 76;
                for (int j = 0; j < attachmentData.length(); j += chunkSize) {
                    int end = Math.min(j + chunkSize, attachmentData.length());
                    String chunk = attachmentData.substring(j, end) + "\r\n";
                    response = handler.sendMessage(channel, chunk);
                    System.out.println(response);
                    if (!channel.isActive()) {
                        throw new Exception("Channel is no longer active");
                    }
                }
            }

            // 发送邮件结束标记
            String endOfMessage = "--" + boundary + "--\r\n.\r\n";
            response = handler.sendMessage(channel, endOfMessage);
            System.out.println(response);
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



