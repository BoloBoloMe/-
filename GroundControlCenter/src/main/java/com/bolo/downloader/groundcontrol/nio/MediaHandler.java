package com.bolo.downloader.groundcontrol.nio;

import com.bolo.downloader.groundcontrol.ClientBootstrap;
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

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.List;
import java.util.Map;

public class MediaHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private static final MyLogger log = LoggerFactory.getLogger(MediaHandler.class);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) throws Exception {
        if (!msg.decoderResult().isSuccess()) {
            ResponseUtil.sendError(ctx, HttpResponseStatus.BAD_REQUEST, msg);
            return;
        }
        final String uri = sanitizeUri(msg.uri());
        if (null == uri) {
            ResponseUtil.sendError(ctx, HttpResponseStatus.BAD_REQUEST, msg);
            return;
        }
        if (!HttpMethod.GET.equals(msg.method())) {
            ResponseUtil.sendText(ctx, HttpResponseStatus.METHOD_NOT_ALLOWED, msg, "请以GET方式访问");
        }
        Map<String, List<String>> params = new QueryStringDecoder(msg.uri()).parameters();
        if ("/pl".equals(uri)) {
            List<String> target = params.get("tar");
            if (target == null || target.size() == 0) {
                ResponseUtil.sendText(ctx, HttpResponseStatus.PAYMENT_REQUIRED, msg, "缺少必要参数！请指定要播放的媒体文件");
                return;
            }
            HttpPlayer.play(ctx, msg, target.get(0), 0);
        } else if ("/fl".equals(uri)) {
            List<String> p = params.get("name");
            String name = null;
            if (p != null && p.size() > 0) {
                name = p.get(0);
            }
            ResponseUtil.sendJSON(ctx, HttpResponseStatus.OK, msg, HttpPlayer.fileListJson(name));
        } else if ("/ssd".equals(uri)) {
            ClientBootstrap.shutdownGracefully();
        } else {
            PageUtil.Page page = PageUtil.findPage(uri, params);
            ByteBuf byteBuf = null;
            try {
                byteBuf = ByteBuffUtils.copy(page.getContent());
                // Build the response object.
                FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, msg.decoderResult().isSuccess() ? HttpResponseStatus.OK : HttpResponseStatus.BAD_REQUEST, byteBuf);
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

    private String sanitizeUri(String uri) throws UnsupportedEncodingException {
        // Decode the path.
        uri = URLDecoder.decode(uri, "UTF-8");
        if (uri.isEmpty() || uri.charAt(0) != '/') {
            return null;
        }
        if (uri.contains("/.") ||
                uri.contains("./") ||
                uri.charAt(0) == '.' || uri.charAt(uri.length() - 1) == '.') {
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
