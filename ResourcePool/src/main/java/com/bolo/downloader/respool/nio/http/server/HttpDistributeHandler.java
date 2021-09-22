package com.bolo.downloader.respool.nio.http.server;

import com.bolo.downloader.respool.nio.http.server.invoke.GeneralMethodInvoker;
import com.bolo.downloader.respool.nio.http.server.invoke.GeneralResultInterpreter;
import com.bolo.downloader.respool.nio.http.server.scan.MethodMapperScanner;
import com.bolo.downloader.respool.nio.http.server.scan.ScanContextHolder;
import com.bolo.downloader.respool.nio.utils.ResponseUtil;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;

import java.util.Objects;

/**
 * http 请求分发控制器，用于支持 @Controller, @RequestMapping 等注解
 */
@ChannelHandler.Sharable
public class HttpDistributeHandler extends ChannelInboundHandlerAdapter {
    private static final GeneralMethodInvoker invoker = new GeneralMethodInvoker(new GeneralResultInterpreter());
    private final String rootPath;

    private HttpDistributeHandler(String rootPath) {
        this.rootPath = rootPath;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        boolean isKeepAlive = false;
        if (appropriateRequest(msg)) {
            FullHttpRequest request = (FullHttpRequest) msg;
            FullHttpResponse response = invoker.invoke(ctx, request);
            HttpUtil.setContentLength(response, response.content().readableBytes());
            ResponseUtil.sendAndCleanupConnection(ctx, request, response, false);
            isKeepAlive = HttpUtil.isKeepAlive(request);
        }
        if (isKeepAlive) {
            ctx.fireChannelRead(msg);
        }
    }

    private boolean appropriateRequest(Object msg) {
        return msg instanceof FullHttpRequest && ((FullHttpRequest) msg).uri().indexOf(rootPath) == 0;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private MethodMapperScanner scanner;
        private String rootPath;

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

        public HttpDistributeHandler build() {
            this.rootPath = formatRootPath();
            ScanContextHolder.set(ScanContextHolder.KEY_ROOT_PATH, this.rootPath);
            scanner.scan();
            scanner = null;
            ScanContextHolder.remove();
            return new HttpDistributeHandler(this.rootPath);
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
