package com.bolo.downloader.respool.nio.http.invoke;

import com.sun.istack.internal.Nullable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;


/**
 * 封装 @RequestMapping 所修饰方法的调用过程
 */
public interface MethodInvoker {
    @Nullable
    ResponseEntity invoke(ChannelHandlerContext ctx, FullHttpRequest request);
}
