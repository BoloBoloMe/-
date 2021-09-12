package com.bolo.downloader.groundcontrol.nio;

import com.bolo.downloader.groundcontrol.ClientBootstrap;
import com.bolo.downloader.groundcontrol.util.FileMap;
import com.bolo.downloader.groundcontrol.util.HttpPlayer;
import com.bolo.downloader.respool.log.LoggerFactory;
import com.bolo.downloader.respool.log.MyLogger;
import com.bolo.downloader.respool.nio.ByteBuffUtils;
import com.bolo.downloader.respool.nio.PageUtil;
import com.bolo.downloader.respool.nio.ResponseUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.util.ReferenceCountUtil;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.*;

public class MediaHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private static final MyLogger log = LoggerFactory.getLogger(MediaHandler.class);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        if (!request.decoderResult().isSuccess()) {
            ResponseUtil.sendError(ctx, HttpResponseStatus.BAD_REQUEST, request);
            return;
        }
        String deUri = URLDecoder.decode(request.uri(), "utf8");
        String uri = sanitizeUri(deUri);
        if (null == uri) {
            ResponseUtil.sendError(ctx, HttpResponseStatus.BAD_REQUEST, request);
            return;
        }
        if (!HttpMethod.GET.equals(request.method())) {
            ResponseUtil.sendText(ctx, HttpResponseStatus.METHOD_NOT_ALLOWED, request, "请以GET方式访问");
        }
        Map<String, List<String>> params = getParamFromUri(deUri);
        if ("/pl".equals(uri)) {
            List<String> target = params.get("tar");
            if (target == null || target.size() == 0) {
                ResponseUtil.sendText(ctx, HttpResponseStatus.PAYMENT_REQUIRED, request, "缺少必要参数！请指定要播放的媒体文件");
                return;
            }
            HttpPlayer.play(ctx, request, target.get(0), HttpPlayer.PATTERN_PLAY);
        } else if ("/dl".equals(uri)) {
            // 下载文件
            List<String> target = params.get("tar");
            if (target == null || target.size() == 0) {
                ResponseUtil.sendText(ctx, HttpResponseStatus.PAYMENT_REQUIRED, request, "缺少必要参数！请指定要播放的媒体文件");
                return;
            }
            HttpPlayer.play(ctx, request, target.get(0), HttpPlayer.PATTERN_DOWLOND);
        } else if ("/fl".equals(uri)) {
            ResponseUtil.sendJSON(ctx, HttpResponseStatus.OK, request, FileMap.fullListJson());
        } else if ("/ssd".equals(uri)) {
            ClientBootstrap.shutdownGracefully();
        } else {
            if ("/pv".equals(uri)) {
                List<String> tar = params.get("tar");
                if (null == tar || tar.size() == 0) {
                    ResponseUtil.sendText(ctx, HttpResponseStatus.PAYMENT_REQUIRED, request, "缺少必要参数！请指定要播放的媒体文件");
                    return;
                }
                String name = tar.get(0);
                String title = name.length() > 30 ? (name.substring(0, 27) + "...") : name;
                if (FileMap.isVideo(name)) {
                    uri = "/page/playVideo.html";
                    params.clear();
                    params.put("p", Arrays.asList(name, name, title, "/pl?tar=" + URLEncoder.encode(name, "utf8")));
                } else if (FileMap.isAudio(name)) {
                    uri = "/page/playAudio.html";
                    params = new HashMap<>();
                    params.put("p", Arrays.asList(name, name, title, "/pl?tar=" + URLEncoder.encode(name, "utf8")));
                } else {
                    ResponseUtil.sendError(ctx, HttpResponseStatus.NOT_FOUND, request);
                    return;
                }
            } else if ("/gamble".equals(uri)) {
                params.clear();
                String name = FileMap.gamble();
                if (name == null) {
                    uri = "/page/index.html";
                } else if (FileMap.isVideo(name)) {
                    uri = "/page/playVideo.html";
                    String title = name.length() > 30 ? (name.substring(0, 27) + "...") : name;
                    params.put("p", Arrays.asList(name, name, title, "/pl?tar=" + URLEncoder.encode(name, "utf8")));
                } else if (FileMap.isAudio(name)) {
                    uri = "/page/playAudio.html";
                    String title = name.length() > 30 ? (name.substring(0, 27) + "...") : name;
                    params.put("p", Arrays.asList(name, name, title, "/pl?tar=" + URLEncoder.encode(name, "utf8")));
                } else {
                    ResponseUtil.sendError(ctx, HttpResponseStatus.NOT_FOUND, request);
                    return;
                }
            }
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
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("操作异常！", cause);
        ctx.close();
    }

    private static String sanitizeUri(String uri) {
        if (uri.isEmpty() || uri.charAt(0) != '/') {
            return null;
        }
        if (uri.contains("/.") ||
                uri.contains("./") ||
                uri.charAt(0) == '.' || uri.charAt(uri.length() - 1) == '.') {
            return null;
        }
        // cut param
        int endIndex = uri.indexOf('?');
        if (endIndex >= 0) {
            uri = uri.substring(0, endIndex);
        }
        return uri.equals("/") ? "/page/index.html" : uri;
    }

    private static Map<String, List<String>> getParamFromUri(String uri) {
        LinkedHashMap<String, List<String>> param = new LinkedHashMap<>();
        int beginIndex = uri.indexOf('?');
        if (beginIndex < 0) {
            return param;
        }
        String[] entryList = uri.substring(beginIndex + 1).split("&");
        for (String entry : entryList) {
            String[] entryArr = entry.split("=");
            if (entryArr.length != 2) continue;
            List<String> list = param.computeIfAbsent(entryArr[0], k -> new LinkedList<>());
            list.add(entryArr[1]);
        }
        return param;
    }
}
