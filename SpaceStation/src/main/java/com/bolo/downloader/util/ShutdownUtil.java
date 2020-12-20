package com.bolo.downloader.util;

import com.bolo.downloader.ServerBootstrap;
import com.bolo.downloader.respool.log.LoggerFactory;
import com.bolo.downloader.respool.log.MyLogger;
import com.bolo.downloader.respool.nio.ResponseUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

public class ShutdownUtil {
    private static final MyLogger log = LoggerFactory.getLogger(ShutdownUtil.class);

    public static boolean handleShutdownReq(ChannelHandlerContext ctx, FullHttpRequest request) {
        SocketAddress socketAddress = ctx.channel().remoteAddress();
        if (socketAddress instanceof InetSocketAddress) {
            InetSocketAddress inetSocketAddress = (InetSocketAddress) socketAddress;
            String clientIP = inetSocketAddress.getAddress().getHostAddress();
            log.info("请求关闭程序, client ip: %s", clientIP);
            if ("127.0.0.1".equals(clientIP) || "localhost".equals(clientIP) || clientIP.contains("0.0.0")) {
                log.info("系统将异步关闭");
                Thread cleaner = new Thread(() -> {
                    try {
                        Thread.sleep(500);
                    } catch (Exception e) {
                    }
                    ServerBootstrap.shutdownGracefully();
                });
                cleaner.setDaemon(false);
                cleaner.setName("ClearThread");
                cleaner.start();
                ResponseUtil.sendText(ctx, HttpResponseStatus.OK, request, "OK");
            } else {
                ResponseUtil.sendText(ctx, HttpResponseStatus.OK, request, "非法操作！");
            }
        }
        return false;
    }
}
