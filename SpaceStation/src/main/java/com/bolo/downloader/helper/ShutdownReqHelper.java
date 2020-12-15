package com.bolo.downloader.helper;

import com.bolo.downloader.Bootstrap;
import com.bolo.downloader.respool.log.LoggerFactory;
import com.bolo.downloader.respool.log.MyLogger;
import com.bolo.downloader.utils.ResponseUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;
import java.util.Map;

public class ShutdownReqHelper {
    private static final MyLogger log = LoggerFactory.getLogger(ShutdownReqHelper.class);

    public static boolean handle(String uri, Map<String, List<String>> params, ChannelHandlerContext ctx, FullHttpRequest request) {
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
                    Bootstrap.shutdownGracefully();
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
