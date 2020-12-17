package test.nio.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;


public class FirstInboundHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        System.out.println("FirstInboundHandler");
        ctx.writeAndFlush("aaaaa");
        ctx.fireChannelReadComplete();
    }
}
