package com.bolo.downloader.groundcontrol.server;

import com.bolo.downloader.groundcontrol.factory.ConfFactory;
import com.bolo.downloader.respool.nio.utils.FileTransferUtil;
import com.bolo.downloader.respool.nio.utils.ResponseUtil;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;

import java.net.URLDecoder;
import java.util.Collections;
import java.util.Objects;

@ChannelHandler.Sharable
public class StaticPageHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private static final StaticPageHandler INSTANCE = new StaticPageHandler();

    private StaticPageHandler() {
    }

    public static StaticPageHandler getInstance() {
        return INSTANCE;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        if (!request.decoderResult().isSuccess()) {
            ResponseUtil.sendError(ctx, HttpResponseStatus.BAD_REQUEST, request);
            return;
        }
        String deUri = URLDecoder.decode(request.uri());
        String uri = sanitizeUri(deUri);
        if (null == uri) {
            ResponseUtil.sendError(ctx, HttpResponseStatus.BAD_REQUEST, request);
            return;
        }
        if (!HttpMethod.GET.equals(request.method())) {
            ResponseUtil.sendText(ctx, HttpResponseStatus.METHOD_NOT_ALLOWED, request, "请以GET方式访问");
        }
        FileTransferUtil.sendFile(ctx, request, getBasicPath() + uri, Collections.emptyMap());
    }

    private static String sanitizeUri(String uri) {
        if (uri.isEmpty()) {
            return null;
        }
        if (uri.contains("/.") ||
                uri.contains("./") ||
                uri.charAt(0) == '.' || uri.charAt(uri.length() - 1) == '.') {
            return null;
        }
        // cut param
        int endIndex = uri.indexOf('?');
        if (endIndex >= 0) {
            uri = uri.substring(0, endIndex);
        }
        return uri.equals("/") ? "/page/index.html" : uri;
    }

    private volatile String basicPath;

    private String getBasicPath() {
        if (Objects.nonNull(basicPath)) {
            return basicPath;
        }
        synchronized (this) {
            basicPath = ConfFactory.get("staticFilePath");
        }
        return basicPath;
    }
}
