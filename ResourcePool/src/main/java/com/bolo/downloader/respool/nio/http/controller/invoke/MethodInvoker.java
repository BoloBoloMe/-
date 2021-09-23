package com.bolo.downloader.respool.nio.http.controller.invoke;

import com.bolo.downloader.respool.nio.http.controller.scan.impl.MethodMapperContainer;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;

import javax.naming.OperationNotSupportedException;


/**
 * 封装 @RequestMapping 所修饰方法的调用过程
 */
public interface MethodInvoker {
    void setMethodMapperContainer(MethodMapperContainer container) throws OperationNotSupportedException;
    FullHttpResponse invoke(ChannelHandlerContext ctx, FullHttpRequest request);
}
