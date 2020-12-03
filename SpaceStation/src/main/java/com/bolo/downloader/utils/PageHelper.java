package com.bolo.downloader.utils;

import com.bolo.downloader.factory.ConfFactory;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;

import java.io.*;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

/**
 * 页面访问助手
 */
public class PageHelper {
    private static String basic = null;
    private static final byte[] PAGE_NOT_FUND = "<!DOCTYPE html><html><head><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width, initial-scale=1, maximum-scale=1\"><title>404</title><link rel=\"stylesheet\" href=\"../layui/css/layui.css\"></head><body><div style=\"width:100%; height:400px;position:absolute; left:50%; top:50%; margin-left: -300px; margin-top: -200px;\"><i class=\"layui-icon layui-icon-face-cry\" style=\"font-size: 200px; color: crimson;\"></i><span style=\"font-size: 50px;\">404:未曾设想的道路</span></div></body></html>".getBytes(Charset.forName("UTF-8"));
    private static final byte[] SERVER_ERROR = "<!DOCTYPE html><html><head>\n<meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width, initial-scale=1, maximum-scale=1\"><title>500</title><link rel=\"stylesheet\" href=\"../layui/css/layui.css\"></head><body><div style=\"width:100%; height:400px;position:absolute; left:50%; top:50%; margin-left: -300px; margin-top: -200px;\"><i class=\"layui-icon layui-icon-face-surprised\" style=\"font-size: 200px; color: crimson;\"></i><span style=\"font-size: 50px;\">ERROR: 一袋米要抗几楼</span></div></body></html>".getBytes(Charset.forName("UTF-8"));

    /**
     * 根据访问路径返回页面内容
     */
    public static boolean toPage(String uri, Map<String, List<String>> params, ChannelHandlerContext ctx, FullHttpRequest request) {
        // Decide whether to close the connection or not.
        boolean keepAlive = HttpUtil.isKeepAlive(request);
        // Build the response object.

        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
                request.decoderResult().isSuccess() ? HttpResponseStatus.OK : HttpResponseStatus.BAD_REQUEST,
                findPage(uri, params));
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
        return keepAlive;
    }

    private static ByteBuf findPage(String uri, Map<String, List<String>> params) {
        ByteBuf byteBuf = null;
        File page;
        if (uri.equals("/v") && params.get("p") != null) {
            String pageName = params.get("p").get(0);
            page = new File(basic == null ? (basic = ConfFactory.get("staticFilePath")) : basic, "page" + File.separator + pageName + ".html");
        } else {
            page = new File(basic == null ? (basic = ConfFactory.get("staticFilePath")) : basic, uri);
        }
        if (page.exists()) {
            try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(uri))) {
                int len = (int) page.length();
                byteBuf = Unpooled.buffer((int) page.length());
                byteBuf.writeBytes(in, len);
            } catch (IOException e) {
                e.printStackTrace();
                byteBuf = Unpooled.copiedBuffer(SERVER_ERROR);
            }
        }
        return byteBuf == null ? Unpooled.copiedBuffer(PAGE_NOT_FUND) : byteBuf;
    }

    private static String getContentType(String uri) {
        String flag = uri.substring(uri.lastIndexOf('.'), uri.length());
        if (flag.equals(".html")) {
            return "text/html";
        }
        if (flag.equals(".css")) {
            return "text/css";
        }
        if (flag.equals(".js")) {
            return "application/javascript";
        }
        if (flag.equals(".ico")) {
            return "application/octet-stream";
        }
        if (flag.equals(".woff2")) {
            return "application/font-woff2";
        }
        return "text/html";
    }

}
