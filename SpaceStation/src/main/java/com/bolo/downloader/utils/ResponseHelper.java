package com.bolo.downloader.utils;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;

public class ResponseHelper {

    protected static final String HTTP_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss zzz";
    protected static final String HTTP_DATE_GMT_TIMEZONE = "GMT";
    protected static final int HTTP_CACHE_SECONDS = 60;

    public static void sendPartialCont(ChannelHandlerContext ctx, FullHttpRequest request) {
        DefaultHttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.PARTIAL_CONTENT);
        // if protocol is http 1.0 , tell the client must by keep alive
        if (request.protocolVersion().equals(HttpVersion.HTTP_1_0)) {
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        }
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
        HttpUtil.setContentLength(response, 0);
        sendAndCleanupConnection(ctx, request, response, false);
    }

    public static void sendError(ChannelHandlerContext ctx, HttpResponseStatus status, FullHttpRequest request) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status);
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
        HttpUtil.setContentLength(response, response.content().readableBytes());
        sendAndCleanupConnection(ctx, request, response, true);
    }

    public static void sendText(ChannelHandlerContext ctx, HttpResponseStatus status, FullHttpRequest request, String text) {
        ByteBuf byteBuf = null;
        try {
            byteBuf = ByteBuffUtils.copy(text, CharsetUtil.UTF_8);
            FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, byteBuf);
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
            HttpUtil.setContentLength(response, response.content().readableBytes());
            sendAndCleanupConnection(ctx, request, response, true);
        } finally {
            if (byteBuf != null && byteBuf.refCnt() > 0) ReferenceCountUtil.safeRelease(byteBuf);
        }
    }

    public static void sendJSON(ChannelHandlerContext ctx, HttpResponseStatus status, FullHttpRequest request, String JSON) {
        ByteBuf json = null;
        try {
            json = ByteBuffUtils.copy(JSON, CharsetUtil.UTF_8);
            FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, json);
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=UTF-8");
            HttpUtil.setContentLength(response, response.content().readableBytes());
            sendAndCleanupConnection(ctx, request, response, false);
        } finally {
            if (json != null && json.refCnt() > 0) ReferenceCountUtil.safeRelease(json);
        }
    }

    /**
     * If Keep-Alive is disabled, attaches "Connection: close" header to the response
     * and closes the connection after the response being sent.
     */
    public static void sendAndCleanupConnection(ChannelHandlerContext ctx, FullHttpRequest request, HttpResponse response, boolean forceClose) {
        final boolean keepAlive = HttpUtil.isKeepAlive(request);
        if (forceClose || !keepAlive) {
            // We're going to close the connection as soon as the response is sent,
            // so we should also make it clear for the client.
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
        } else if (request.protocolVersion().equals(HttpVersion.HTTP_1_0)) {
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        }
        ChannelFuture flushPromise = ctx.writeAndFlush(response);
        if (forceClose || !keepAlive) {
            // Close the connection as soon as the response is sent.
            flushPromise.addListener(ChannelFutureListener.CLOSE);
        }
        if (forceClose) {
            ctx.close();
        }
    }
}
