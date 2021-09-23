package com.bolo.downloader.groundcontrol.server;

import com.bolo.downloader.respool.nio.utils.ByteBuffUtils;
import com.bolo.downloader.respool.nio.utils.PageUtil;
import com.bolo.downloader.respool.nio.utils.ResponseUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.util.ReferenceCountUtil;

import java.net.URLDecoder;
import java.util.Collections;

public class StaticPageHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private static final StaticPageHandler INSTANCE = new StaticPageHandler();

    private StaticPageHandler() {
    }

    public static StaticPageHandler getInstance(){
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
        PageUtil.Page page = PageUtil.findPage(uri, Collections.emptyMap());
        ByteBuf byteBuf = null;
        try {
            byteBuf = ByteBuffUtils.copy(page.getContent());
            // Build the response object.
            FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, request.decoderResult().isSuccess() ? HttpResponseStatus.OK : HttpResponseStatus.BAD_REQUEST, byteBuf);
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, page.getContentType());
            // Write the response and flush.
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        } finally {
            if (byteBuf != null && byteBuf.refCnt() > 0) ReferenceCountUtil.safeRelease(byteBuf);
        }
    }

    private static String sanitizeUri(String uri) {
        if (uri.isEmpty() || uri.indexOf("/page/") != 0) {
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
}
