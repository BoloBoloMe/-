package com.bolo.downloader.respool.nio.http;

import com.bolo.downloader.respool.nio.http.scan.MethodMapperScanner;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * http 请求分发控制器，用于支持 @Controller, @RequestMapping 等注解
 */
@ChannelHandler.Sharable
public class HttpDistributeHandler extends ChannelInboundHandlerAdapter {
    private HttpDistributeHandler() {
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ctx.fireChannelRead(msg);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private MethodMapperScanner scanner;

        private Builder() {
        }

        public Builder setScanner(MethodMapperScanner scanner) {
            this.scanner = scanner;
            return this;
        }

        public HttpDistributeHandler build() {
            scanner.scan();
            scanner = null;
            return new HttpDistributeHandler();
        }
    }

}
