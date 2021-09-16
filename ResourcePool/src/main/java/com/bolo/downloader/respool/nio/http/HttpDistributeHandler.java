package com.bolo.downloader.respool.nio.http;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpMethod;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

/**
 * http 请求分发控制器，用于支持 @Controller, @RequestMapping 等注解
 */
@ChannelHandler.Sharable
public class HttpDistributeHandler extends ChannelInboundHandlerAdapter {
    private ConcurrentHashMap<String, HttpRequestHandler> httpRequestHandlerSet = new ConcurrentHashMap<>();

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        ctx.fireChannelRead(msg);
    }


    private static class HttpRequestHandler {
        final private String path;
        final private HttpMethod httpMethod;
        final private Method targetMethod;
        final private Object targetInstance;
        private final Class<?> targetClass;


        public HttpRequestHandler(String path, HttpMethod httpMethod, Method targetMethod, Object targetInstance, Class<?> targetClass) {
            this.path = path;
            this.httpMethod = httpMethod;
            this.targetMethod = targetMethod;
            this.targetInstance = targetInstance;
            this.targetClass = targetClass;
        }

        public String getPath() {
            return path;
        }

        public HttpMethod getHttpMethod() {
            return httpMethod;
        }

        public Method getTargetMethod() {
            return targetMethod;
        }

        public Object getTargetInstance() {
            return targetInstance;
        }

        public Class<?> getTargetClass() {
            return targetClass;
        }
    }

}
