package com.example.backend.Client;

import com.example.backend.Client.Handler.ImapClientHandler;
import com.example.backend.DTO.MailDTO;
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

import java.time.LocalDateTime;

@Component
public class ImapClient {

    private final String host;
    private final int port;
    private Channel channel;
    private EventLoopGroup group;

    public ImapClient(
            @Value("${server.imap.host}") String host,
            @Value("${server.imap.port}") int port) {
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
                        pipeline.addLast(new ImapClientHandler());
                    }
                });

        ChannelFuture future = bootstrap.connect(host, port).sync();
        channel = future.channel();
    }

    public void loginCommand(String userEmail, String password) throws InterruptedException {
        if (channel != null && channel.isActive()) {
            ImapClientHandler handler = channel.pipeline().get(ImapClientHandler.class);
            // 发送 Login 命令并等待响应
            String response = handler.sendCommand(channel, "Login " + userEmail + " " + password);
            System.out.println("IMAP return：" + response);
        }
    }

    public MailDTO fetchCommand(long mailId) throws InterruptedException {
        if (channel != null && channel.isActive()) {
            ImapClientHandler handler = channel.pipeline().get(ImapClientHandler.class);
            String response = handler.sendCommand(channel, "FETCH " + mailId);
            // 解析响应
            return parseFetchResponse(response, mailId);
        }
        else{
            return null;
        }
    }

    // 解析 FETCH 响应
    private MailDTO parseFetchResponse(String response, long mailId) {
        String[] lines = response.split("\r\n");
        MailDTO mailDTO = new MailDTO();
        String content = null;
        String buffer = null;
        int getCount = 0;
        int contentLength = 0;

        for (String line : lines) {
            if (line.startsWith("* " + mailId + " FETCH (BODY[] {")) {
                // Extract content length (optional, if needed for validation)
                contentLength = Integer.parseInt(line.split("\\{")[1].split("}")[0]);
            } else if (line.startsWith("FROM ")) {
                buffer = line.substring(5);
                getCount += buffer.length();
                mailDTO.setSender_email(buffer);
            } else if (line.startsWith("TO ")) {
                buffer = line.substring(3);
                getCount += buffer.length();
                mailDTO.setReceiver_email(buffer);
            } else if (line.startsWith("SUBJECT ")) {
                buffer = line.substring(8);
                getCount += buffer.length();
                mailDTO.setSubject(buffer);
            } else if (line.startsWith("DATE ")) {
                buffer = line.substring(5);
                getCount += buffer.length();
                mailDTO.setCreate_at(LocalDateTime.parse(buffer));
            } else if (!line.startsWith("*") && !line.startsWith("1 OK") && getCount < contentLength) {
                // Assume this is part of the email content
                if (content == null) {
                    content = line;
                } else {
                    content += "\n" + line;
                    getCount += 1;
                }
                getCount += line.length();
            } else {
                System.out.println("IMAP return：" + response);
            }
        }

        // 保存内容
        if (content != null) {
            mailDTO.setContent(content);
        }

        // 打印解析结果
        System.out.println("Parsed Mail Info:");
        System.out.println("Sender: " + mailDTO.getSender_email());
        System.out.println("Receiver: " + mailDTO.getReceiver_email());
        System.out.println("Subject: " + mailDTO.getSubject());
        System.out.println("Date: " + mailDTO.getCreate_at());
        System.out.println("Content: " + mailDTO.getContent());

        return mailDTO;
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