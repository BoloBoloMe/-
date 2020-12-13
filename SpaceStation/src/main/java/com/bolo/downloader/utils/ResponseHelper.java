package com.bolo.downloader.utils;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;

public class ResponseHelper {

    public static void sendError(ChannelHandlerContext ctx, HttpResponseStatus status, FullHttpRequest request) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status);
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
        HttpUtil.setContentLength(response, response.content().readableBytes());
        sendAndCleanupConnection(ctx, request, response, true);
    }

    public static void sendText(ChannelHandlerContext ctx, HttpResponseStatus status, FullHttpRequest request, String text) {
        ByteBuf byteBuf = ByteBuffUtils.copy(text, CharsetUtil.UTF_8);
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, byteBuf);
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
        HttpUtil.setContentLength(response, response.content().readableBytes());
        sendAndCleanupConnection(ctx, request, response, true);
    }

    public static void sendJSON(ChannelHandlerContext ctx, HttpResponseStatus status, FullHttpRequest request, String JSON) {
        ByteBuf json = ByteBuffUtils.copy(JSON, CharsetUtil.UTF_8);
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, json);
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=UTF-8");
        HttpUtil.setContentLength(response, response.content().readableBytes());
        sendAndCleanupConnection(ctx, request, response, false);
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
