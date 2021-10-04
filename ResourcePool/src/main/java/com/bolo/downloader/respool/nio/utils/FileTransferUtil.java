package com.bolo.downloader.respool.nio.utils;

import com.bolo.downloader.respool.log.LoggerFactory;
import com.bolo.downloader.respool.log.MyLogger;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedFile;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpResponseStatus.PARTIAL_CONTENT;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class FileTransferUtil {
    private static final MyLogger log = LoggerFactory.getLogger(FileTransferUtil.class);
    public static final String PATTERN_DOWLOND = "attachment;filename=";
    public static final String PATTERN_PLAY = "inline";

    public static void sendFile(ChannelHandlerContext ctx, FullHttpRequest request, String absolutePaths, Map<CharSequence, Object> headers) {
        File file;
        if (absolutePaths == null || !(file = new File(absolutePaths)).exists() || file.isHidden()) {
            ResponseUtil.sendError(ctx, HttpResponseStatus.NOT_FOUND, request);
            return;
        }
        try {
            RandomAccessFile fileAcc = new RandomAccessFile(file, "r");
            // 支持HTTP 1.1 断点续传请求头 reange:bytes=0-1
            String range = request.headers().get(HttpHeaderNames.RANGE);
            final long start, end, transLen, fileLen = fileAcc.length(), lastIndex = fileLen - 1;
            if (null != range && range.matches("bytes=[0-9]+-[1-9]*[0-9]*")) {
                int index_0 = range.indexOf('=') + 1, index_1 = range.indexOf('-');
                start = Long.parseLong(range.substring(index_0, index_1));
                end = '-' == range.charAt(range.length() - 1) ? fileLen - 1 : Long.parseLong(range.substring(index_1 + 1));
            } else {
                start = 0;
                end = lastIndex;
            }
            transLen = end - start + 1;
            HttpResponse response = new DefaultHttpResponse(HTTP_1_1, lastIndex == end ? OK : PARTIAL_CONTENT);
            setCustomizationHeaders(headers, response);
            setContentHaders(response, file, start, end, transLen, fileLen);
            setDateAndCacheHeaders(response, file);
            // Write the initial line and the header.
            ctx.write(response);
            // Write the content.
            ChannelFuture sendFileFuture;
            if (ctx.pipeline().get(SslHandler.class) == null) {
                sendFileFuture =
                        ctx.write(new DefaultFileRegion(fileAcc.getChannel(), start, transLen), ctx.newProgressivePromise());
                // Write the end marker.
                 ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
                sendFileFuture.addListener(channelProgressiveFutureListener);
            } else {
                sendFileFuture =
                        ctx.writeAndFlush(new HttpChunkedInput(new ChunkedFile(fileAcc, start, transLen, 8192)),
                                ctx.newProgressivePromise());
                // HttpChunkedInput will write the end marker (LastHttpContent) for us.
                sendFileFuture.addListener(channelProgressiveFutureListener);
            }
        } catch (Exception e) {
            log.error("文件传输异常！", e);
            ResponseUtil.sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, request);
        }
    }

    private static final ChannelProgressiveFutureListener channelProgressiveFutureListener = new ChannelProgressiveFutureListener() {
        @Override
        public void operationProgressed(ChannelProgressiveFuture future, long progress, long total) {
            if (total < 0) { // total unknown
                log.info("[file transfers] 传送数据: %d byte", progress);
            } else {
                log.info("[file transfers] 传送数据: %d/%d byte ", progress, total);
            }
        }

        @Override
        public void operationComplete(ChannelProgressiveFuture future) {
            log.info(" [file transfers] 文件传送结束.");
        }
    };


    private static void setCustomizationHeaders(Map<CharSequence, Object> headers, HttpResponse response) {
        if (null == headers || headers.isEmpty()) {
            return;
        }
        headers.forEach((name, value) -> {
            if (value instanceof Iterable) {
                response.headers().set(name, ((Iterable<?>) value));
            } else {
                response.headers().set(name, value);
            }
        });
    }

    private static void setContentHaders(HttpResponse response, File file, long start, long end, long transLen, long fileLen) throws IOException {
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, Files.probeContentType(file.toPath()));
        response.headers().set(HttpHeaderNames.ACCEPT_RANGES, "bytes");
        response.headers().set(HttpHeaderNames.CONTENT_RANGE, "bytes " + start + "-" + end + "/" + fileLen);
        HttpUtil.setContentLength(response, transLen);
    }


    private static final String HTTP_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss zzz";
    private static final String HTTP_DATE_GMT_TIMEZONE = "GMT";
    private static final int HTTP_CACHE_SECONDS = 60;

    private static void setDateAndCacheHeaders(HttpResponse response, File fileToCache) {
        SimpleDateFormat dateFormatter = new SimpleDateFormat(HTTP_DATE_FORMAT, Locale.CHINA);
        dateFormatter.setTimeZone(TimeZone.getTimeZone(HTTP_DATE_GMT_TIMEZONE));

        // Date header
        Calendar time = new GregorianCalendar();
        response.headers().set(HttpHeaderNames.DATE, dateFormatter.format(time.getTime()));

        // Add cache headers
        time.add(Calendar.SECOND, HTTP_CACHE_SECONDS);
        response.headers().set(HttpHeaderNames.EXPIRES, dateFormatter.format(time.getTime()));
        response.headers().set(HttpHeaderNames.CACHE_CONTROL, "private, max-age=" + HTTP_CACHE_SECONDS);
        response.headers().set(
                HttpHeaderNames.LAST_MODIFIED, dateFormatter.format(new Date(fileToCache.lastModified())));
    }
}
