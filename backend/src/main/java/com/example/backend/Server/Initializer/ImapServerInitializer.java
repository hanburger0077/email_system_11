package com.example.backend.Server.Initializer;

import com.example.backend.Server.Handler.ImapServerHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.timeout.IdleStateHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class ImapServerInitializer extends ChannelInitializer<SocketChannel> {

    private final ApplicationContext applicationContext;

    private final int READER_IDLE_TIMEOUT = 30 * 60;

    @Autowired
    public ImapServerInitializer(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast(new StringDecoder());
        pipeline.addLast(new StringEncoder());
        pipeline.addLast(new IdleStateHandler(READER_IDLE_TIMEOUT, 0, 0));
        // 添加一个处理器来处理空闲事件
        pipeline.addLast(new ChannelInboundHandlerAdapter() {
                             @Override
                             public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
                                 if (evt instanceof io.netty.handler.timeout.IdleStateEvent) {
                                     io.netty.handler.timeout.IdleStateEvent e = (io.netty.handler.timeout.IdleStateEvent) evt;
                                     if (e.state() == io.netty.handler.timeout.IdleState.READER_IDLE) {
                                         System.out.println("Idle timeout, closing connection");
                                         pipeline.get(ImapServerHandler.class).clearAndClose(ctx);
                                         ctx.close();
                                     }
                                 }
                             }
                         });
        // 每次初始化时创建一个新的 ImapServerHandler 实例
        pipeline.addLast(applicationContext.getBean(ImapServerHandler.class));
    }


}