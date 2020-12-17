package com.bolo.downloader.nio.handler;

import com.alibaba.fastjson.JSON;
import com.bolo.downloader.factory.DownloaderFactory;
import com.bolo.downloader.utils.PageUtil;
import com.bolo.downloader.utils.ShutdownUtil;
import com.bolo.downloader.respool.log.LoggerFactory;
import com.bolo.downloader.respool.log.MyLogger;
import com.bolo.downloader.station.Downloader;
import com.bolo.downloader.utils.ByteBuffUtils;
import com.bolo.downloader.utils.ResponseUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.handler.codec.http.multipart.MemoryAttribute;
import io.netty.util.ReferenceCountUtil;

import java.io.*;
import java.net.URLDecoder;
import java.util.*;

public class HttpServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private static final MyLogger log = LoggerFactory.getLogger(HttpServerHandler.class);
    private FullHttpRequest request;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        this.request = request;
        if (!request.decoderResult().isSuccess()) {
            ResponseUtil.sendError(ctx, HttpResponseStatus.BAD_REQUEST, request);
            return;
        }
        final String uri = sanitizeUri(request.uri());
        if (null == uri) {
            ResponseUtil.sendError(ctx, HttpResponseStatus.BAD_REQUEST, request);
            return;
        }
        Map<String, List<String>> params;
        if (HttpMethod.GET.equals(request.method())) {
            // 读操作通过 GET METHOD 访问
            params = new QueryStringDecoder(request.uri()).parameters();
            get(uri, params, ctx, request);
        } else if (HttpMethod.POST.equals(request.method())) {
            // 写操作通过 POST METHOD 访问
            params = new HashMap<>();
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
            post(uri, params, ctx, request);
        } else {
            ResponseUtil.sendError(ctx, HttpResponseStatus.METHOD_NOT_ALLOWED, request);
        }
        // 如果客户端请求，就立即关闭连接
        if (ctx.channel().isOpen() && !HttpUtil.isKeepAlive(request)) {
            ctx.writeAndFlush(ByteBuffUtils.empty()).addListener(ChannelFutureListener.CLOSE);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("操作异常！", cause);
        if (ctx.channel().isOpen() && ctx.channel().isWritable()) {
            try {
                ResponseUtil.sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, request);
            } catch (Exception e) {
                log.error("异常后响应-再次捕获异常！", e);
            }
        } else {
            ctx.close();
        }
    }

    private void post(String uri, Map<String, List<String>> params, ChannelHandlerContext ctx, FullHttpRequest request) {
        Downloader downloader = DownloaderFactory.getObject();
        if (uri.equals("/task/add")) {
            if (params.get("url") == null || params.get("url").size() <= 0) {
                ResponseUtil.sendText(ctx, HttpResponseStatus.OK, request, "请输入下载地址");
                return;
            }
            String targetAddr;
            try {
                targetAddr = new String(Base64.getDecoder().decode(params.get("url").get(0)));
            } catch (Exception e) {
                ResponseUtil.sendText(ctx, HttpResponseStatus.OK, request, "下载地址解析失败！" + e.getMessage());
                return;
            }
            String result = downloader.addTask(targetAddr) == 1 ? "添加成功" : "任务已在列表中！";
            ResponseUtil.sendText(ctx, HttpResponseStatus.OK, request, result);
        } else if (uri.equals("/task/clear")) {
            downloader.clearTasks();
            ResponseUtil.sendText(ctx, HttpResponseStatus.OK, request, "列表已清空");
        } else if (uri.equals("/df")) {
            download(ctx, params, request);
        } else {
            ResponseUtil.sendError(ctx, HttpResponseStatus.NOT_FOUND, request);
        }
    }

    private void get(String uri, Map<String, List<String>> params, ChannelHandlerContext ctx, FullHttpRequest request) {
        log.info("get uri:%s,params:%s", uri, JSON.toJSON(params));
        Downloader downloader = DownloaderFactory.getObject();
        switch (uri) {
            case "/task/list": {
                Map<String, String> result = downloader.listTasks();
                ResponseUtil.sendJSON(ctx, HttpResponseStatus.OK, request, JSON.toJSONString(result));
                break;
            }
            case "/video/list": {
                String result = downloader.listVideo();
                ResponseUtil.sendJSON(ctx, HttpResponseStatus.OK, request, result);
                break;
            }
            case "/ssd":
                ShutdownUtil.handleShutdownReq(ctx, request);
                break;
            case "/df":
                download(ctx, params, request);
                break;
            default:
                toPage(uri, params, ctx, request);
                break;
        }
    }

    private void toPage(String uri, Map<String, List<String>> params, ChannelHandlerContext ctx, FullHttpRequest request) {
        PageUtil.Page page = PageUtil.findPage(uri, params);
        ByteBuf byteBuf = null;
        try {
            byteBuf = ByteBuffUtils.copy(page.getContent());
            // Build the response object.
            FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, request.decoderResult().isSuccess() ? HttpResponseStatus.OK : HttpResponseStatus.BAD_REQUEST, byteBuf);
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, page.getContentType());
            // Write the response and flush.
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        } finally {
            if (byteBuf != null && byteBuf.refCnt() > 0) ReferenceCountUtil.safeRelease(byteBuf);
        }
    }

    private String sanitizeUri(String uri) throws UnsupportedEncodingException {
        // Decode the path.
        uri = URLDecoder.decode(uri, "UTF-8");
        if (uri.isEmpty() || uri.charAt(0) != '/') {
            return null;
        }
        // Simplistic dumb security check.
        if (uri.contains("/.") ||
                uri.contains("./") ||
                uri.charAt(0) == '.' || uri.charAt(uri.length() - 1) == '.' /*||
                INSECURE_URI.matcher(uri).matches()*/) {
            return null;
        }
        // cut param
        int endIndex;
        if ((endIndex = uri.indexOf('?')) >= 0) {
            uri = uri.substring(0, endIndex);
        }
        return uri.equals("/") ? "/page/index.html" : uri;
    }

    private void download(ChannelHandlerContext ctx, Map<String, List<String>> params, FullHttpRequest request) {
        try {
            FileDownloadHelper.handle(params,ctx,request);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}