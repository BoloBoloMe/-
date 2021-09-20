package com.bolo.downloader.respool.nio.http.server;

import com.bolo.downloader.respool.nio.http.server.invoke.GeneralMethodInvoker;
import com.bolo.downloader.respool.nio.http.server.invoke.GeneralResultInterpreter;
import com.bolo.downloader.respool.nio.http.server.scan.MethodMapperScanner;
import com.bolo.downloader.respool.nio.utils.ResponseUtil;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;

/**
 * http 请求分发控制器，用于支持 @Controller, @RequestMapping 等注解
 */
@ChannelHandler.Sharable
public class HttpDistributeHandler extends ChannelInboundHandlerAdapter {
    private static final GeneralMethodInvoker invoker = new GeneralMethodInvoker(new GeneralResultInterpreter());

    private HttpDistributeHandler() {
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        boolean isKeepAlive = false;
        if (msg instanceof FullHttpRequest) {
            FullHttpRequest request = (FullHttpRequest) msg;
            FullHttpResponse response = invoker.invoke(ctx, request);
            HttpUtil.setContentLength(response, response.content().readableBytes());
            ResponseUtil.sendAndCleanupConnection(ctx, request, response, false);
            isKeepAlive = !HttpHeaderValues.CLOSE.toString().equals(response.headers().get(HttpHeaderNames.CONNECTION));
        }
        if (isKeepAlive) {
            ctx.fireChannelRead(msg);
        }
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
