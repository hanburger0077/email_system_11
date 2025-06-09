package com.example.backend.Server.Initializer;

import com.example.backend.Server.Handler.SmtpServerHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class SmtpServerInitializer extends ChannelInitializer<SocketChannel> {

    private final ApplicationContext applicationContext;

    private final int READER_IDLE_TIMEOUT = 5;

    @Autowired
    public SmtpServerInitializer(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast(new StringDecoder());
        pipeline.addLast(new StringEncoder());
        pipeline.addLast(new IdleStateHandler(READER_IDLE_TIMEOUT, 0, 0));
        pipeline.addLast(new ChannelInboundHandlerAdapter() {
            @Override
            public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
                if (evt instanceof IdleStateEvent) {
                    IdleStateEvent e = (IdleStateEvent) evt;
                    if (e.state() == IdleState.READER_IDLE) {
                        System.out.println("No data received for " + READER_IDLE_TIMEOUT + " seconds, closing connection.");
                        ctx.close(); // 关闭连接
                    }
                }
            }
        });
        // 每次初始化时创建一个新的 SmtpServerHandler 实例
        pipeline.addLast(applicationContext.getBean(SmtpServerHandler.class));
    }
}