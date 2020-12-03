package com.bolo.downloader.nio;

import com.bolo.downloader.factory.ReqQueueFactory;
import com.bolo.downloader.utils.ResponseHelper;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.handler.codec.http.multipart.MemoryAttribute;

import java.io.*;
import java.net.URLDecoder;
import java.util.*;
import java.util.regex.Pattern;

/**
 * 根据请求地址，访问页面或下载文件
 */
public class HttpServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private FullHttpRequest request;

    @Override
    public void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        this.request = request;
        if (!request.decoderResult().isSuccess()) {
            ResponseHelper.sendError(ctx, HttpResponseStatus.BAD_REQUEST, request);
            return;
        }
        final String uri = sanitizeUri(request.uri());
        if (null == uri) {
            ResponseHelper.sendError(ctx, HttpResponseStatus.BAD_REQUEST, request);
            return;
        }
        if (HttpMethod.GET.equals(request.method())) {
            Map<String, List<String>> params = new QueryStringDecoder(request.uri()).parameters();
            ReqQueueFactory.get().add(new ReqRecord(request.method(), uri, params, ctx, request));
        } else if (HttpMethod.POST.equals(request.method())) {
            Map<String, List<String>> params = new HashMap<>();
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
            ReqQueueFactory.get().add(new ReqRecord(request.method(), uri, params, ctx, request));
        } else {
            ResponseHelper.sendError(ctx, HttpResponseStatus.METHOD_NOT_ALLOWED, request);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        if (ctx.channel().isActive()) {
            ResponseHelper.sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, request);
        }
    }


    private static final Pattern INSECURE_URI = Pattern.compile(".*[<>&\"].*");

    private static String sanitizeUri(String uri) throws UnsupportedEncodingException {
        // Decode the path.
        uri = URLDecoder.decode(uri, "UTF-8");
        if (uri.isEmpty() || uri.charAt(0) != '/') {
            return null;
        }
        // Simplistic dumb security check.
        if (uri.contains("/.") ||
                uri.contains("./") ||
                uri.charAt(0) == '.' || uri.charAt(uri.length() - 1) == '.' ||
                INSECURE_URI.matcher(uri).matches()) {
            return null;
        }
        // cut param
        int endIndex;
        if ((endIndex = uri.indexOf('?')) >= 0) {
            uri = uri.substring(0, endIndex);
        }

        return uri.equals("/") ? "/page/index.html" : uri;
    }
}
