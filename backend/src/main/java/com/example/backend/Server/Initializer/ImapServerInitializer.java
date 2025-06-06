package com.example.backend.Server.Initializer;

import com.example.backend.Server.Handler.ImapServerHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class ImapServerInitializer extends ChannelInitializer<SocketChannel> {

    private final ApplicationContext applicationContext;

    @Autowired
    public ImapServerInitializer(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast(new StringDecoder());
        pipeline.addLast(new StringEncoder());
        // 每次初始化时创建一个新的 ImapServerHandler 实例
        pipeline.addLast(applicationContext.getBean(ImapServerHandler.class));
    }
}