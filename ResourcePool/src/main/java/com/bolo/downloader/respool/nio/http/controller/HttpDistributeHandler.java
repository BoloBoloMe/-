package com.bolo.downloader.respool.nio.http.controller;

import com.bolo.downloader.respool.nio.http.controller.invoke.MethodInvoker;
import com.bolo.downloader.respool.nio.http.controller.scan.MethodMapperScanner;
import com.bolo.downloader.respool.nio.http.controller.scan.impl.ScanContextHolder;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;

import java.util.Objects;
import java.util.Optional;

/**
 * http 请求分发控制器，用于支持 @Controller, @RequestMapping 等注解
 */
@ChannelHandler.Sharable
public class HttpDistributeHandler extends ChannelInboundHandlerAdapter {
    private final MethodInvoker invoker;
    private final String rootPath;

    private HttpDistributeHandler(String rootPath, MethodInvoker invoker) {
        this.rootPath = rootPath;
        this.invoker = invoker;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (appropriateRequest(msg)) {
            FullHttpRequest request = (FullHttpRequest) msg;
            FullHttpResponse response = invoker.invoke(ctx, request);
            HttpUtil.setContentLength(response, response.content().readableBytes());
            sendAndCleanupConnection(ctx, request, response);
        } else {
            ctx.fireChannelRead(msg);
        }
    }

    private boolean appropriateRequest(Object msg) {
        return msg instanceof FullHttpRequest && ((FullHttpRequest) msg).uri().indexOf(rootPath) == 0;
    }


    /**
     * If Keep-Alive is disabled, attaches "Connection: close" header to the response
     * and closes the connection after the response being sent.
     */
    public static void sendAndCleanupConnection(ChannelHandlerContext ctx, FullHttpRequest request, HttpResponse response) {
        final boolean keepAlive = HttpUtil.isKeepAlive(request);
        if (!keepAlive) {
            // We're going to close the connection as soon as the response is sent,
            // so we should also make it clear for the client.
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
        } else if (request.protocolVersion().equals(HttpVersion.HTTP_1_0)) {
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        }
        ChannelFuture flushPromise = ctx.writeAndFlush(response);
        if (!keepAlive) {
            // Close the connection as soon as the response is sent.
            flushPromise.addListener(ChannelFutureListener.CLOSE);
        }
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private MethodMapperScanner scanner;
        private String rootPath;
        private MethodInvoker methodInvoker;

        private Builder() {
        }

        public Builder setScanner(MethodMapperScanner scanner) {
            this.scanner = scanner;
            return this;
        }

        public Builder setRootPath(String rootPath) {
            this.rootPath = rootPath;
            return this;
        }

        public Builder setMethodInvoker(MethodInvoker methodInvoker) {
            this.methodInvoker = methodInvoker;
            return this;
        }

        public HttpDistributeHandler build() {
            try {
                this.rootPath = formatRootPath();
                ScanContextHolder.set(ScanContextHolder.KEY_ROOT_PATH, this.rootPath);
                ScanContextHolder.set(ScanContextHolder.KEY_METHOD_INVOKER, this.methodInvoker);
                scanner.scan();
                scanner = null;
                Optional<MethodInvoker> invokerOpt = ScanContextHolder.getValue(ScanContextHolder.KEY_METHOD_INVOKER, MethodInvoker.class);
                if (invokerOpt.isPresent()) {
                    return new HttpDistributeHandler(this.rootPath, invokerOpt.get());
                } else {
                    throw new RuntimeException("Failed to create a HttpDistributeHandler instance. because missing invoker");
                }
            } finally {
                ScanContextHolder.remove();
            }
        }

        private String formatRootPath() {
            String path = this.rootPath;
            if (Objects.isNull(path) || path.isEmpty()) {
                path = "/";
            } else {
                if (path.charAt(0) != '/') {
                    path = "/" + path;
                }
                if (path.charAt(path.length() - 1) != '/') {
                    path = path + "/";
                }
            }
            return path;
        }
    }

}
