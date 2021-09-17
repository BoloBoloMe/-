package com.bolo.downloader.respool.nio.http.invoke;

import com.bolo.downloader.respool.log.LoggerFactory;
import com.bolo.downloader.respool.log.MyLogger;
import com.bolo.downloader.respool.nio.http.HttpDistributeHandler;
import com.bolo.downloader.respool.nio.http.scan.MethodMapper;
import com.bolo.downloader.respool.nio.http.scan.MethodMapperContainer;
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

    @Override
    public ResponseEntity invoke(ChannelHandlerContext ctx, FullHttpRequest request) {
        if (!request.decoderResult().isSuccess()) {
            return new ResponseEntity(HttpResponseStatus.BAD_REQUEST);
        }
        final String uri = request.uri();
        final HttpMethod requestMethod = request.method();
        if (Objects.isNull(uri) || Objects.isNull(requestMethod)) {
            return new ResponseEntity( HttpResponseStatus.BAD_REQUEST, "invalid request");
        }
        final MethodMapper methodMapper = MethodMapperContainer.get(uri);
        if (Objects.isNull(methodMapper)) {
            return new ResponseEntity(HttpResponseStatus.NOT_FOUND, "invalid path: " + uri);
        }
        final Object instance = methodMapper.getTargetInstance();
        final Method method = methodMapper.getTargetMethod();
        Optional<HttpMethod> allowedMethod = methodMapper.getIfExist(requestMethod);
        if (!allowedMethod.isPresent()) {
            return new ResponseEntity(HttpResponseStatus.METHOD_NOT_ALLOWED, "allowed method " + methodMapper.getAllowedMethods());
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
