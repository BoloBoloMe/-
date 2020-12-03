package com.bolo.downloader.utils;

import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedFile;
import io.netty.util.internal.SystemPropertyUtil;

import javax.activation.MimetypesFileTypeMap;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 文件下载助手
 */
public class FileDownloadHelper extends ResponseHelper {
    public static boolean download(String uri, Map<String, List<String>> params, ChannelHandlerContext ctx, FullHttpRequest request) {
        final boolean keepAlive = HttpUtil.isKeepAlive(request);
        // Convert file separators.
        uri = uri.replace('/', File.separatorChar);
        // Convert to absolute path.
        final String path = SystemPropertyUtil.get("user.dir") + File.separator + uri;
        File file = new File(path);
        if (file.isHidden() || !file.exists()) {
            sendError(ctx, HttpResponseStatus.NOT_FOUND, request);
            return keepAlive;
        }
        if (!file.isFile()) {
            sendError(ctx, HttpResponseStatus.FORBIDDEN, request);
            return keepAlive;
        }
        // Cache Validation
        String ifModifiedSince = request.headers().get(HttpHeaderNames.IF_MODIFIED_SINCE);
        if (ifModifiedSince != null && !ifModifiedSince.isEmpty()) {
            SimpleDateFormat dateFormatter = new SimpleDateFormat(HTTP_DATE_FORMAT, Locale.US);
            Date ifModifiedSinceDate = null;
            try {
                ifModifiedSinceDate = dateFormatter.parse(ifModifiedSince);
            } catch (ParseException e) {
                sendError(ctx, HttpResponseStatus.BAD_REQUEST, request);
                return keepAlive;
            }
            // Only compare up to the second because the datetime format we send to the client
            // does not have milliseconds
            long ifModifiedSinceDateSeconds = ifModifiedSinceDate.getTime() / 1000;
            long fileLastModifiedSeconds = file.lastModified() / 1000;
            if (ifModifiedSinceDateSeconds == fileLastModifiedSeconds) {
                sendNotModified(ctx, request);
                return keepAlive;
            }
        }
        // 需要跳过的字节数
        int skipLen = 0;
        List<String> lenPar = params.get("l");
        if (lenPar != null && lenPar.size() > 0) {
            try {
                skipLen = Integer.valueOf(lenPar.get(0));
            } catch (Exception e) {
                sendError(ctx, HttpResponseStatus.BAD_REQUEST, request);
                return keepAlive;
            }
        }
        long fileLength;
        RandomAccessFile raf;
        try {
            raf = new RandomAccessFile(file, "r");
            fileLength = raf.length();
        } catch (FileNotFoundException ignore) {
            sendError(ctx, HttpResponseStatus.NOT_FOUND, request);
            return keepAlive;
        } catch (IOException e) {
            sendError(ctx, HttpResponseStatus.BAD_REQUEST, request);
            return keepAlive;
        }
        if (skipLen < 0 || skipLen > fileLength) {
            sendError(ctx, HttpResponseStatus.BAD_REQUEST, request);
            return keepAlive;
        }
        HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        HttpUtil.setContentLength(response, fileLength);
        setContentTypeHeader(response, file);
        setDateAndCacheHeaders(response, file, request);

        if (!keepAlive) {
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
        } else if (request.protocolVersion().equals(HttpVersion.HTTP_1_0)) {
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        }

        // Write the initial line and the header.
        ctx.write(response);

        // Write the content.
        ChannelFuture sendFileFuture;
        ChannelFuture lastContentFuture;
        if (ctx.pipeline().get(SslHandler.class) == null) {
            sendFileFuture = ctx.write(new DefaultFileRegion(raf.getChannel(), skipLen, fileLength - skipLen), ctx.newProgressivePromise());
            // Write the end marker.
            lastContentFuture = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
        } else {
            try {
                sendFileFuture = ctx.writeAndFlush(new HttpChunkedInput(new ChunkedFile(raf, skipLen, fileLength - skipLen, 8192)),
                        ctx.newProgressivePromise());
            } catch (IOException e) {
                sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, request);
                return keepAlive;
            }
            // HttpChunkedInput will write the end marker (LastHttpContent) for us.
            lastContentFuture = sendFileFuture;
        }

        sendFileFuture.addListener(new ChannelProgressiveFutureListener() {
            @Override
            public void operationProgressed(ChannelProgressiveFuture future, long progress, long total) {
                if (total < 0) { // total unknown
                    System.err.println(future.channel() + " Transfer progress: " + progress);
                } else {
                    System.err.println(future.channel() + " Transfer progress: " + progress + " / " + total);
                }
            }

            @Override
            public void operationComplete(ChannelProgressiveFuture future) {
                System.err.println(future.channel() + " Transfer complete.");
            }
        });

        // Decide whether to close the connection or not.
        if (!keepAlive) {
            // Close the connection when the whole content is written out.
            lastContentFuture.addListener(ChannelFutureListener.CLOSE);
        }

        return keepAlive;
    }

    /**
     * Sets the content type header for the HTTP Response
     *
     * @param response HTTP response
     * @param file     file to extract content type
     */
    private static void setContentTypeHeader(HttpResponse response, File file) {
        MimetypesFileTypeMap mimeTypesMap = new MimetypesFileTypeMap();
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, mimeTypesMap.getContentType(file.getPath()));
    }

    /**
     * Sets the Date and Cache headers for the HTTP Response
     *
     * @param response    HTTP response
     * @param fileToCache file to extract content type
     */
    private static void setDateAndCacheHeaders(HttpResponse response, File fileToCache, FullHttpRequest request) {
        SimpleDateFormat dateFormatter = new SimpleDateFormat(HTTP_DATE_FORMAT, Locale.US);
        dateFormatter.setTimeZone(TimeZone.getTimeZone(HTTP_DATE_GMT_TIMEZONE));

        // Date header
        Calendar time = new GregorianCalendar();
        response.headers().set(HttpHeaderNames.DATE, dateFormatter.format(time.getTime()));

        // Add cache headers
        time.add(Calendar.SECOND, HTTP_CACHE_SECONDS);
        response.headers().set(HttpHeaderNames.EXPIRES, dateFormatter.format(time.getTime()));
        response.headers().set(HttpHeaderNames.CACHE_CONTROL, "private, max-age=" + HTTP_CACHE_SECONDS);
        response.headers().set(HttpHeaderNames.LAST_MODIFIED, dateFormatter.format(new Date(fileToCache.lastModified())));
    }

    /**
     * When file timestamp is the same as what the browser is sending up, send a "304 Not Modified"
     *
     * @param ctx Context
     */
    private static void sendNotModified(ChannelHandlerContext ctx, FullHttpRequest request) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_MODIFIED, Unpooled.EMPTY_BUFFER);
        setDateHeader(response);
        sendAndCleanupConnection(ctx, request, response);
    }

    /**
     * Sets the Date header for the HTTP response
     *
     * @param response HTTP response
     */
    private static void setDateHeader(FullHttpResponse response) {
        SimpleDateFormat dateFormatter = new SimpleDateFormat(HTTP_DATE_FORMAT, Locale.US);
        dateFormatter.setTimeZone(TimeZone.getTimeZone(HTTP_DATE_GMT_TIMEZONE));

        Calendar time = new GregorianCalendar();
        response.headers().set(HttpHeaderNames.DATE, dateFormatter.format(time.getTime()));
    }
}
