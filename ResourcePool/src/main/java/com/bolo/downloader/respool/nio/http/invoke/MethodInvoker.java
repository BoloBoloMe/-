package com.bolo.downloader.respool.nio.http.invoke;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;


/**
 * 封装 @RequestMapping 所修饰方法的调用过程
 */
public interface MethodInvoker {
    FullHttpResponse invoke(ChannelHandlerContext ctx, FullHttpRequest request);
}
