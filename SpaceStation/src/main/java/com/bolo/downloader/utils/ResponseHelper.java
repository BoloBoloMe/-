package com.bolo.downloader.utils;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;

public class ResponseHelper {

    protected static final String HTTP_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss zzz";
    protected static final String HTTP_DATE_GMT_TIMEZONE = "GMT";
    protected static final int HTTP_CACHE_SECONDS = 60;


    public static void sendOK(ChannelHandlerContext ctx, FullHttpRequest request) {
        DefaultHttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        // if protocol is http 1.0 , tell the client must by keep alive
        if (request.protocolVersion().equals(HttpVersion.HTTP_1_0)) {
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        }
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
        HttpUtil.setContentLength(response, 0);
        sendAndCleanupConnection(ctx, request, response);
    }

    public static void sendError(ChannelHandlerContext ctx, HttpResponseStatus status, FullHttpRequest request) {
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, status, Unpooled.copiedBuffer("Failure: " + status + "\r\n", CharsetUtil.UTF_8));
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
        HttpUtil.setContentLength(response, response.content().readableBytes());
        sendAndCleanupConnection(ctx, request, response);
    }

    /**
     * If Keep-Alive is disabled, attaches "Connection: close" header to the response
     * and closes the connection after the response being sent.
     */
    protected static void sendAndCleanupConnection(ChannelHandlerContext ctx, FullHttpRequest request, HttpResponse response) {
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
}
