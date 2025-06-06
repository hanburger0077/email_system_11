package com.example.backend.Server;

import com.example.backend.Server.Config.ServerPortConfig;
import com.example.backend.Server.Initializer.ImapServerInitializer;
import com.example.backend.Server.Initializer.SmtpServerInitializer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class Server {

    private static final Logger logger = LoggerFactory.getLogger(Server.class);

    private final ServerPortConfig config;
    private final SmtpServerInitializer smtpServerInitializer;
    private final ImapServerInitializer imapServerInitializer;

    @Autowired
    public Server(ServerPortConfig config, SmtpServerInitializer smtpServerInitializer, ImapServerInitializer imapServerInitializer) {
        this.config = config;
        this.smtpServerInitializer = smtpServerInitializer;
        this.imapServerInitializer = imapServerInitializer;
    }

    public void run() throws Exception {
        logger.info("Starting server...");
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            // 启动SMTP服务器
            CompletableFuture<Void> smtpFuture = CompletableFuture.runAsync(() -> {
                try {
                    startServer(bossGroup, workerGroup, config.getSmtpPort(), smtpServerInitializer);
                } catch (InterruptedException e) {
                    logger.error("SMTP server interrupted", e);
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    logger.error("SMTP server error", e);
                }
            });

            // 启动IMAP服务器
            CompletableFuture<Void> imapFuture = CompletableFuture.runAsync(() -> {
                try {
                    startServer(bossGroup, workerGroup, config.getImapPort(), imapServerInitializer);
                } catch (InterruptedException e) {
                    logger.error("IMAP server interrupted", e);
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    logger.error("IMAP server error", e);
                }
            });

            // 等待两个服务器都启动完成
            CompletableFuture.allOf(smtpFuture, imapFuture).join();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
            logger.info("Server shutdown gracefully");
        }
    }

    private void startServer(EventLoopGroup bossGroup, EventLoopGroup workerGroup, int port, ChannelInitializer<SocketChannel> initializer) throws InterruptedException {
        ServerBootstrap b = new ServerBootstrap();
        b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(initializer)
                .option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000); // 设置读取超时时间为 5000 毫秒

        logger.info("Binding server to port: {}", port);
        ChannelFuture f = b.bind(port).sync();
        logger.info("Server started on port {}", port);

        f.channel().closeFuture().sync();
        logger.debug("Channel closed for port {}", port);
    }
}