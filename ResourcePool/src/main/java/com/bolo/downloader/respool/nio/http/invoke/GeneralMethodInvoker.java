package com.bolo.downloader.respool.nio.http.invoke;

import com.bolo.downloader.respool.log.LoggerFactory;
import com.bolo.downloader.respool.log.MyLogger;
import com.bolo.downloader.respool.nio.http.HttpDistributeHandler;
import com.bolo.downloader.respool.nio.http.RequestContextHolder;
import com.bolo.downloader.respool.nio.http.scan.MethodMapper;
import com.bolo.downloader.respool.nio.http.scan.MethodMapperContainer;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.handler.codec.http.multipart.MemoryAttribute;

import java.lang.reflect.Method;
import java.util.*;

/**
 * 方法调用的共用处理过程
 */
public class GeneralMethodInvoker implements MethodInvoker {
    private static final MyLogger log = LoggerFactory.getLogger(HttpDistributeHandler.class);
    private final ResultInterpreter interpreter;

    public GeneralMethodInvoker(ResultInterpreter interpreter) {
        this.interpreter = interpreter;
    }


    @Override
    public FullHttpResponse invoke(ChannelHandlerContext ctx, FullHttpRequest request) {
        Object responseEntity = doInvoke(ctx, request);
        return interpreter.interpret(responseEntity);
    }

    private Object doInvoke(ChannelHandlerContext ctx, FullHttpRequest request) {
        if (!request.decoderResult().isSuccess()) {
            return new ResponseEntity<>(HttpResponseStatus.BAD_REQUEST);
        }
        final String uri = request.uri();
        final HttpMethod requestMethod = request.method();
        if (Objects.isNull(uri) || Objects.isNull(requestMethod)) {
            return new ResponseEntity<>(HttpResponseStatus.BAD_REQUEST, "invalid request");
        }
        final MethodMapper methodMapper = MethodMapperContainer.get(uri);
        if (Objects.isNull(methodMapper)) {
            return new ResponseEntity<>(HttpResponseStatus.NOT_FOUND, "invalid path: " + uri);
        }
        final Object instance = methodMapper.getTargetInstance();
        final Method method = methodMapper.getTargetMethod();
        Optional<HttpMethod> allowedMethod = methodMapper.getIfExist(requestMethod);
        if (!allowedMethod.isPresent()) {
            return new ResponseEntity<>(HttpResponseStatus.METHOD_NOT_ALLOWED, "allowed method " + methodMapper.getAllowedMethods());
        }
        try {
            Map<String, List<String>> parameterMap = getParameters(ctx, request, uri, requestMethod);
            Object[] parameterList = alignParameters(ctx, request, parameterMap, method);
            RequestContextHolder.setParameters(parameterMap);
            return method.invoke(instance, parameterList);
        } catch (Exception e) {
            log.error("http request handler invoke failed. method=" + method.getName() + ",parameters={}." + Collections.emptyList(), e);
        } finally {
            RequestContextHolder.remove();
        }
        return new ResponseEntity<>(HttpResponseStatus.INTERNAL_SERVER_ERROR);
    }

    private Map<String, List<String>> getParameters(ChannelHandlerContext ctx, FullHttpRequest request, String uri, HttpMethod requestMethod) {
        Map<String, List<String>> params = new HashMap<>();
        if (HttpMethod.GET.equals(requestMethod)) {
            int beginIndex = uri.indexOf('?');
            if (beginIndex < 0) {
                return params;
            }
            String[] entryList = uri.substring(beginIndex + 1).split("&");
            for (String entry : entryList) {
                String[] entryArr = entry.split("=");
                if (entryArr.length != 2) {
                    continue;
                }
                List<String> list = params.computeIfAbsent(entryArr[0], k -> new LinkedList<>());
                list.add(entryArr[1]);
            }
            return params;
        } else if (HttpMethod.POST.equals(requestMethod)) {
            HttpPostRequestDecoder decoder = new HttpPostRequestDecoder(new DefaultHttpDataFactory(false), request);
            List<InterfaceHttpData> parmList = decoder.getBodyHttpDatas();
            for (InterfaceHttpData parm : parmList) {
                if (parm.getHttpDataType() == InterfaceHttpData.HttpDataType.Attribute) {
                    MemoryAttribute data = (MemoryAttribute) parm;
                    List<String> values;
                    if (null == (values = params.get(data.getName()))) {
                        values = new ArrayList<>();
                        values.add(data.getValue());
                        params.put(data.getName(), values);
                    } else {
                        values.add(data.getValue());
                    }
                }
            }
        }
        return params;
    }


    private Object[] alignParameters(ChannelHandlerContext ctx, FullHttpRequest request, Map<String, List<String>> parameters, Method method) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        Object[] parameter = new Object[parameterTypes.length];
        for (int index = 0; index < parameterTypes.length; index++) {
            Class<?> pClass = parameterTypes[index];
            if (pClass.isInstance(ctx)) {
                parameter[index] = ctx;
            }
            if (pClass.isInstance(request)) {
                parameter[index] = request;
            }
        }
        return parameter;
    }
}
