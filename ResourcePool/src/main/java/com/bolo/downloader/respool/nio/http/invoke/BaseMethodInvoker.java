package com.bolo.downloader.respool.nio.http.invoke;

import com.bolo.downloader.respool.log.LoggerFactory;
import com.bolo.downloader.respool.log.MyLogger;
import com.bolo.downloader.respool.nio.http.HttpDistributeHandler;
import com.bolo.downloader.respool.nio.utils.ResponseUtil;
import com.sun.istack.internal.Nullable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;

import java.lang.reflect.Method;
import java.util.*;

/**
 * 方法调用的共用处理过程
 */
abstract public class BaseMethodInvoker implements MethodInvoker {
    private static final MyLogger log = LoggerFactory.getLogger(HttpDistributeHandler.class);

    @Nullable
    public ResponseEntity invoke(Object instance, Method method, HttpMethod[] allowedMethods, ChannelHandlerContext ctx, FullHttpRequest request) {
        if (!request.decoderResult().isSuccess()) {
            return new ResponseEntity(HttpResponseStatus.BAD_REQUEST);
        }
        final String uri = request.uri();
        final HttpMethod requestMethod = request.method();
        if (Objects.isNull(uri) || Objects.isNull(requestMethod)) {
            ResponseUtil.sendError(ctx, HttpResponseStatus.BAD_REQUEST, request);
        }
        if (Objects.isNull(instance) || Objects.isNull(method)) {
            return new ResponseEntity(HttpResponseStatus.NOT_FOUND, "invalid path: " + uri);
        }
        if (Arrays.binarySearch(allowedMethods, requestMethod, HttpMethod::compareTo) >= 0) {
            new ResponseEntity(HttpResponseStatus.METHOD_NOT_ALLOWED, "allowed methods  " + Arrays.toString(allowedMethods));
        }
        try {
            final Object[] parameters = alignParameters(getParameters(ctx, request), method);
            Object result = method.invoke(instance, parameters);
            return interpretResponseEntity(result);
        } catch (Exception e) {
            log.error("http request handler invoke failed. method=" + method.getName() + ",parameters={}." + Collections.emptyList(), e);
        }
        return new ResponseEntity(HttpResponseStatus.INTERNAL_SERVER_ERROR);
    }

    abstract Map<String, List<String>> getParameters(ChannelHandlerContext ctx, FullHttpRequest request);

    @Nullable
    abstract ResponseEntity interpretResponseEntity(@Nullable Object result);


    private Object[] alignParameters(Map<String, List<String>> parameters, Method method) {
        return new Object[]{};
    }
}
