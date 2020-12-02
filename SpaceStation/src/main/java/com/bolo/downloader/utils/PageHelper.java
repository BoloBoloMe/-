package com.bolo.downloader.utils;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;

import java.util.List;
import java.util.Map;

/**
 * 页面访问助手
 */
public class PageHelper {
    /**
     * 根据访问路径返回页面内容
     */
    public static void toPage(String uri, Map<String, List<String>> params, ChannelHandlerContext ctx, FullHttpRequest request) {
        // Decide whether to close the connection or not.
        boolean keepAlive = HttpUtil.isKeepAlive(request);
        // Build the response object.

        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, request.decoderResult().isSuccess() ? HttpResponseStatus.OK : HttpResponseStatus.BAD_REQUEST, findPage(uri, params));
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, getContentType(uri));

        if (keepAlive) {
            // Add 'Content-Length' header only for a keep-alive connection.
            response.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
            // Add keep alive header as per:
            // - https://www.w3.org/Protocols/HTTP/1.1/draft-ietf-http-v11-spec-01.html#Connection
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        }

        // Write the response and flush.
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    private static ByteBuf findPage(String uri, Map<String, List<String>> params) {
        return Unpooled.copiedBuffer(("path is " + uri).getBytes());
    }

    private static String getContentType(String uri) {
        return "";
    }

}
