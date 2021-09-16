package com.bolo.downloader.respool.nio.http.invoke;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;

import java.lang.reflect.Method;

/**
 * 封装 @RequestMapping 所修饰方法的调用过程
 */
public interface MethodInvoker {
    ResponseEntity invoke(Object instance, Method method, ChannelHandlerContext ctx, FullHttpRequest request);
}
