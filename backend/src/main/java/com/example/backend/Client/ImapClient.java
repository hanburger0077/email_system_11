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
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


@Component
public class ImapClient {

    private final String host;
    private final int port;
    private Channel channel;
    private EventLoopGroup group;
    private ScheduledExecutorService scheduler;
    int HEARTBEAT_period = 30;

    @Setter
    private SseEmitter sseEmitter;


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
                        ImapClientHandler handler = new ImapClientHandler();
                        // 设置事件回调
                        handler.setEventCallback(event -> {
                            handleNewMailEvent(event);
                        });

                        pipeline.addLast(handler);
                    }
                });

        ChannelFuture future = bootstrap.connect(host, port).sync();
        channel = future.channel();
    }


    public void disconnect() {
        // 停止心跳
        //stopHeartbeat();
        if (channel != null) {
            channel.close();
        }
        if (group != null) {
            group.shutdownGracefully();
        }
    }


    // 处理新邮件事件
    private void handleNewMailEvent(String event) {
        System.out.println("New mail event received: " + event);
        // 通过SSE发送事件给客户端
        if (sseEmitter != null) {
            try {
                String message = "New mail arrived: " + event;
                sseEmitter.send(message, MediaType.TEXT_EVENT_STREAM);
            } catch (IOException e) {
                sseEmitter.completeWithError(e);
            }
        }
    }


    public void loginCommand(String userEmail, String password) throws InterruptedException {
        if (channel != null && channel.isActive()) {
            ImapClientHandler handler = channel.pipeline().get(ImapClientHandler.class);
            // 发送 Login 命令并等待响应
            String response = handler.sendCommand(channel, "LOGIN " + userEmail + " " + password);
            System.out.println("IMAP return：" + response);
        }
        // 启动心跳机制
        //startHeartbeat();
    }


    public MailDTO fetchCommand(String DorS, long mailId) throws InterruptedException {
        if (channel != null && channel.isActive()) {
            ImapClientHandler handler = channel.pipeline().get(ImapClientHandler.class);
            String response = handler.sendCommand(channel, "FETCH "+ DorS + " " + mailId);
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
            if (line.startsWith("* " + mailId + " FETCH (DATA[] {")) {
                // Extract content length (optional, if needed for validation)
                contentLength = Integer.parseInt(line.split("\\{")[1].split("}")[0]);
            } else if (line.startsWith("ID ")) {
                buffer = line.substring(3);
                getCount += buffer.length();
                mailDTO.setMail_id(Long.parseLong(buffer));
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
            } else if (line.startsWith("READ ")) {
                buffer = line.substring(5);
                getCount += buffer.length();
                mailDTO.setRead(Short.parseShort(buffer));
            } else if (line.startsWith("S_STAR ")) {
                buffer = line.substring(7);
                getCount += buffer.length();
                mailDTO.setSender_star(Short.parseShort(buffer));
            } else if (line.startsWith("R_STAR ")) {
                buffer = line.substring(7);
                getCount += buffer.length();
                mailDTO.setReceiver_star(Short.parseShort(buffer));
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
        System.out.println("Id: " + mailDTO.getMail_id());
        System.out.println("Sender: " + mailDTO.getSender_email());
        System.out.println("Receiver: " + mailDTO.getReceiver_email());
        System.out.println("Subject: " + mailDTO.getSubject());
        System.out.println("Date: " + mailDTO.getCreate_at());
        System.out.println("Content: " + mailDTO.getContent());
        System.out.println("READ: " + mailDTO.getRead());
        System.out.println("S_STAR: " + mailDTO.getSender_star());
        System.out.println("R_STAR: " + mailDTO.getReceiver_star());
        return mailDTO;
    }


    public void selectCommand(String mailbox) throws InterruptedException {
        if (channel != null && channel.isActive()) {
            ImapClientHandler handler = channel.pipeline().get(ImapClientHandler.class);
            // 发送 SELECT 命令并等待响应
            String response = handler.sendCommand(channel, "SELECT " + mailbox);
            String[] lines= response.split("\r\n");
            for(String line : lines) {
                System.out.println("IMAP return：" + line);
            }
        }
    }


    public List<Long> searchCommand(String from, String to, String subject, String body, LocalDateTime since, String unseen, boolean sender_star, boolean receiver_star) throws InterruptedException {
        if (channel != null && channel.isActive()) {
            ImapClientHandler handler = channel.pipeline().get(ImapClientHandler.class);
            StringBuffer command = new StringBuffer("SEARCH");
            int count = 0;
            if (from != null){
                command.append(" FROM:").append(from);
                count++;
            }
            if (to != null){
                command.append(" TO:").append(to);
                count++;
            }
            if (subject != null){
                command.append(" SUBJECT:").append(subject);
                count++;
            }
            if (body != null){
                command.append(" BODY:").append(body);
                count++;
            }
            if (since != null){
                command.append(" SINCE:").append(since);
                count++;
            }
            //已读和未读
            if (unseen != null) {
                if (unseen.equals("UNSEEN")){
                    command.append(" UNSEEN");
                    count++;
                }
                else if(unseen.equals("SEEN")) {
                    command.append(" SEEN");
                    count++;
                }
            }
            if (sender_star){
                command.append(" S_STAR");
                count++;
            }
            if (receiver_star){
                command.append(" R_STAR");
                count++;
            }
            if(count == 0){
                command.append(" ALL");
            }
            // 发送 SEARCH 命令并等待响应
            String response = handler.sendCommand(channel, command.toString());
            String[] lines= response.split("\r\n");
            List<Long> mailId = null;
            if(lines.length == 1) {
                System.out.println("IMAP return: " + lines[0]);
            }
            else {
                if (lines[0].startsWith("* SEARCH ")) {
                    System.out.println("IMAP return: " + lines[0]);
                    mailId = new ArrayList<>();
                    String[] ids = lines[0].substring(9).split(" ");
                    for(String id : ids) {
                        mailId.add(Long.parseLong(id));
                    }

                }
                else {
                    System.out.println("SEARCH FAILED");
                }
                System.out.println("IMAP return: " + lines[1]);
            }
            return mailId;
        }
        else {
            return null;
        }
    }


    public void storeCommand(long mailId, String sign, String op) throws InterruptedException {
        if (channel != null && channel.isActive()) {
            ImapClientHandler handler = channel.pipeline().get(ImapClientHandler.class);
            // 发送 SELECT 命令并等待响应
            String response = handler.sendCommand(channel, "STORE " + mailId + " " + op + " " + sign);
            System.out.println(response);
        }
    }


    public void idleCommand() throws InterruptedException {
        if (channel != null && channel.isActive()) {
            ImapClientHandler handler = channel.pipeline().get(ImapClientHandler.class);
            String response = handler.sendCommand(channel, "IDLE");
            System.out.println("IMAP return：" + response);
        }
    }


    public void deleteCommand(long mailId) throws InterruptedException {
        if (channel != null && channel.isActive()) {
            ImapClientHandler handler = channel.pipeline().get(ImapClientHandler.class);
            String response = handler.sendCommand(channel, "DELETE " + mailId);
            System.out.println("IMAP return：" + response);
        }
    }


    public void draftCommand(long mailId, String from, String to, String subject, String content) throws InterruptedException {
        if (channel != null && channel.isActive()) {
            ImapClientHandler handler = channel.pipeline().get(ImapClientHandler.class);
            String command = "DRAFT\r\n"
                            + from + "\r\n"
                            + to + "\r\n"
                            + subject + "\r\n"
                            + content + "\r\n"
                            + mailId;
            String response = handler.sendCommand(channel, command);
            System.out.println("IMAP return：" + response);
        }
    }


    public void sendEvent(String event) {
        if (sseEmitter != null) {
            try {
                sseEmitter.send(event, MediaType.TEXT_EVENT_STREAM);
            } catch (IOException e) {
                sseEmitter.completeWithError(e);
            }
        }
    }


    //心跳机制，在超时时间之前发布心跳
    public void startHeartbeat() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            try {
                sendNoop();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Heartbeat interrupted: " + e.getMessage());
            }
        }, 0, HEARTBEAT_period, TimeUnit.SECONDS);
    }


    public void stopHeartbeat() {
        if (scheduler != null) {
            scheduler.shutdown();
        }
    }

    public void sendNoop() throws InterruptedException {
        if (channel != null && channel.isActive()) {
            ImapClientHandler handler = channel.pipeline().get(ImapClientHandler.class);
            String response = handler.sendCommand(channel, "NOOP");
            System.out.println("NOOP response: " + response);
        }
    }
}