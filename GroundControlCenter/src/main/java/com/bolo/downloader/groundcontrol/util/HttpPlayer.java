package com.bolo.downloader.groundcontrol.util;

import com.bolo.downloader.groundcontrol.dict.StoneMapDict;
import com.bolo.downloader.groundcontrol.factory.ConfFactory;
import com.bolo.downloader.groundcontrol.factory.StoneMapFactory;
import com.bolo.downloader.respool.log.LoggerFactory;
import com.bolo.downloader.respool.log.MyLogger;
import com.bolo.downloader.respool.nio.ResponseUtil;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedFile;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class HttpPlayer {
    private static final MyLogger log = LoggerFactory.getLogger(HttpPlayer.class);
    private static final ConcurrentHashMap<String, File> fileList = new ConcurrentHashMap<>();
    private static final Timer flushTimer = new Timer(true);

    public static void startFlushTask() {
        flushTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                log.info("定期扫描文件目录");
                String mediaPaths = ConfFactory.get("mediaPath");
                if (null != mediaPaths) {
                    String[] paths = mediaPaths.split(",");
                    File[] dirs = new File[paths.length];
                    for (int i = 0; i < paths.length; i++) {
                        dirs[i] = new File(paths[i]);
                    }
                    scan(dirs);
                }
            }
        }, 0L, 300000L);
    }

    public static void shutdownFlushTask() {
        flushTimer.cancel();
        flushTimer.purge();
    }

    public static String fileListJson(String name) {
        StringBuilder result = new StringBuilder().append('[');
        for (String key : fileList.keySet()) {
            result.append('"');
            if (name == null) {
                result.append(key);
            } else if (key.contains(name)) {
                result.append(key);
            }
            result.append('"').append(',');
        }
        int lastIndex = result.length() - 1;
        if (result.lastIndexOf(",") == lastIndex) {
            result.deleteCharAt(lastIndex);
        }
        result.append(']');
        return result.toString();
    }

    public static void play(ChannelHandlerContext ctx, FullHttpRequest request, String target, long skip) {
        File file = fileList.get(target);
        if (file == null || file.exists() || file.isHidden()) {
            ResponseUtil.sendError(ctx, HttpResponseStatus.NOT_FOUND, request);
            return;
        }
        try {
            RandomAccessFile fileAcc = new RandomAccessFile(file, "r");
            final long fileLen = fileAcc.length();
            final long transLen = fileLen - skip;
            HttpResponse response = new DefaultHttpResponse(HTTP_1_1, OK);
            HttpUtil.setContentLength(response, transLen);
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, Files.probeContentType(file.toPath()));
            response.headers().set(HttpHeaderNames.CONTENT_DISPOSITION, "attachment;filename=DownloadFile");
            setDateAndCacheHeaders(response, file);
            // Write the initial line and the header.
            ctx.write(response);
            // Write the content.
            ChannelFuture sendFileFuture;
            ChannelFuture lastContentFuture;
            if (ctx.pipeline().get(SslHandler.class) == null) {
                sendFileFuture =
                        ctx.write(new DefaultFileRegion(fileAcc.getChannel(), skip, transLen), ctx.newProgressivePromise());
                // Write the end marker.
                lastContentFuture = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
                sendFileFuture.addListener(channelProgressiveFutureListener);
            } else {
                sendFileFuture =
                        ctx.writeAndFlush(new HttpChunkedInput(new ChunkedFile(fileAcc, skip, transLen, 8192)),
                                ctx.newProgressivePromise());
                // HttpChunkedInput will write the end marker (LastHttpContent) for us.
                lastContentFuture = sendFileFuture;
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

    private static final String HTTP_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss zzz";
    private static final String HTTP_DATE_GMT_TIMEZONE = "GMT";
    private static final int HTTP_CACHE_SECONDS = 60;

    private static void setDateAndCacheHeaders(HttpResponse response, File fileToCache) {
        SimpleDateFormat dateFormatter = new SimpleDateFormat(HTTP_DATE_FORMAT, Locale.US);
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


    final static private String VIDEO_NAME_PATTERN = ".+(\\.mp4|\\.webm|\\.wmv|\\.avi|\\.dat|\\.asf|\\.mpeg|\\.mpg|\\.rm|\\.rmvb|\\.ram|\\.flv|\\.3gp|\\.mov|\\.divx|\\.dv|\\.vob|\\.mkv|\\.qt|\\.cpk|\\.fli|\\.flc|\\.f4v|\\.m4v|\\.mod|\\.m2t|\\.swf|\\.mts|\\.m2ts|\\.3g2|\\.mpe|\\.ts|\\.div|\\.lavf|\\.dirac){1}";

    private static void scan(File[] paths) {
        for (File target : paths) {
            String name = target.getName();
            if (target.isDirectory()) {
                File[] child = target.listFiles();
                if (child != null) {
                    scan(child);
                }
            } else if (target.isFile() && !fileList.containsKey(name) && !target.isHidden() && Pattern.matches(VIDEO_NAME_PATTERN, name.toLowerCase())) {
                fileList.put(name, target);
            }
        }
    }
}
