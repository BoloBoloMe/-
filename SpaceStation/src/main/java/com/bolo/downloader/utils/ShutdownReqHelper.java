package com.bolo.downloader.utils;

import com.bolo.downloader.factory.ReqQueueFactory;
import com.bolo.downloader.nio.ReqRecord;
import com.bolo.downloader.respool.log.LoggerFactory;
import com.bolo.downloader.respool.log.MyLogger;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;
import java.util.Map;

public class ShutdownReqHelper {
    private static final MyLogger log = LoggerFactory.getLogger(ShutdownReqHelper.class);

    public static void shutdown(String uri, Map<String, List<String>> params, ChannelHandlerContext ctx, FullHttpRequest request) {
        SocketAddress socketAddress = ctx.channel().remoteAddress();
        if (socketAddress instanceof InetSocketAddress) {
            InetSocketAddress inetSocketAddress = (InetSocketAddress) socketAddress;
            String clientIP = inetSocketAddress.getAddress().getHostAddress();
            log.info("请求关闭程序, client ip: %s", clientIP);
            if ("127.0.0.1".equals(clientIP) || "localhost".equals(clientIP) || clientIP.contains("0.0.0")) {
                log.info("系统将异步关闭");
                ReqQueueFactory.get().add(new ReqRecord(request.method(), uri, params, ctx, request));
                ResponseHelper.sendText(ctx, HttpResponseStatus.OK, request, "OK");
            } else {
                ResponseHelper.sendText(ctx, HttpResponseStatus.OK, request, "非法操作！");
            }
        }
    }
}
