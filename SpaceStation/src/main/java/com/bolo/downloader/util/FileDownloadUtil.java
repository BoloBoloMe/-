package com.bolo.downloader.util;

import com.bolo.downloader.factory.ConfFactory;
import com.bolo.downloader.respool.log.LoggerFactory;
import com.bolo.downloader.respool.log.MyLogger;
import com.bolo.downloader.respool.nio.utils.ByteBuffUtils;
import com.bolo.downloader.respool.nio.utils.ResponseUtil;
import com.bolo.downloader.sync.Record;
import com.bolo.downloader.sync.SyncState;
import com.bolo.downloader.sync.Synchronizer;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedFile;

import java.io.File;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.*;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class FileDownloadUtil {
    private static final MyLogger log = LoggerFactory.getLogger(FileDownloadUtil.class);
    private static final FullHttpResponse equalsResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, ByteBuffUtils.empty());

    static {
        // init equals response
        equalsResponse.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
        equalsResponse.headers().set("st", "0");
        equalsResponse.headers().set("vs", "0");
        equalsResponse.headers().set("fn", "");
        HttpUtil.setContentLength(equalsResponse, 0);
    }

    public static void handle(Map<String, List<String>> params, ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        final int serverVer = Synchronizer.getCurrVer();
        // get params
        List<String> cvs = params.get("cv");
        List<String> els = params.get("el");
        if (cvs == null || cvs.size() == 0 || els == null || els.size() == 0) {
            ResponseUtil.sendError(ctx, HttpResponseStatus.PAYMENT_REQUIRED, request);
            return;
        }
        final int clientVer, expectedLen;
        try {
            clientVer = Integer.parseInt(cvs.get(0));
            expectedLen = Integer.parseInt(els.get(0));
        } catch (Exception e) {
            ResponseUtil.sendError(ctx, HttpResponseStatus.PAYMENT_REQUIRED, request);
            return;
        }
        if (clientVer < 0 || expectedLen < -1) {
            ResponseUtil.sendError(ctx, HttpResponseStatus.BAD_REQUEST, request);
            return;
        }
        if (serverVer < clientVer) {
            ResponseUtil.sendAndCleanupConnection(ctx, request, equalsResponse, false);
            return;
        }
        // expectedLen == -1, 删除文件
        if (expectedLen == -1) {
            // 文件已经同步完毕,更新当前文件记录的状态
            Record currRecord = Synchronizer.getRecord(null, clientVer, null, null, null);
            if (null != currRecord) Synchronizer.isDownloaded(currRecord.getFileName());
            // 请求下一个需要同步的文件
            Record nextRecord = getNextRecord(clientVer);
            // 如果客户端版本号之后的所有版本都找不到文件,返回 equalsResponse
            if (nextRecord == null) {
                ResponseUtil.sendAndCleanupConnection(ctx, request, equalsResponse, false);
            } else {
                ResponseUtil.sendAndCleanupConnection(ctx, request, createNextFileResp(nextRecord, "1"), false);
            }
            Synchronizer.flush();
            return;
        }
        // expectedLen == 0, 请求新文件
        if (expectedLen == 0) {
            // 请求下一个需要同步的文件
            Record nextRecord = getNextRecord(clientVer);
            // 如果客户端版本号之后的所有版本都找不到文件,返回 equalsResponse
            if (nextRecord == null) {
                ResponseUtil.sendAndCleanupConnection(ctx, request, equalsResponse, false);
            } else {
                ResponseUtil.sendAndCleanupConnection(ctx, request, createNextFileResp(nextRecord, "1"), false);
            }
            return;
        }
        // expectedLen > 0, 下载文件
        File file;
        Record record = Synchronizer.getRecord(null, clientVer, null, null, null);
        if (record == null || SyncState.LOSE.equals(record.getState()) ||
                !(file = new File(ConfFactory.get("videoPath") + File.separator + record.getFileName())).exists()) {
            // 要下载的文件已丢失，尝试寻找下一个可下载的文件
            record = getNextRecord(clientVer);
            if (record == null) {
                // 暂无需要下载的文件,返回 equalsResponse
                ResponseUtil.sendAndCleanupConnection(ctx, request, equalsResponse, false);
            } else {
                ResponseUtil.sendAndCleanupConnection(ctx, request, createNextFileResp(record, "3"), false);
            }
        } else {
            // 要下载的文件还在，下载当前文件
            download(ctx, request, file, record.getMd5());
        }
    }

    private static void download(ChannelHandlerContext ctx, FullHttpRequest request, File file, String md5) throws Exception {
        RandomAccessFile fileAcc = new RandomAccessFile(file, "r");
        final long fileLen = fileAcc.length(), start, end, transLen;
        // 支持HTTP 1.1 断点续传请求头 range: bytes=0-1
        String range = request.headers().get(HttpHeaderNames.RANGE);
        if (null != range && range.matches("bytes=[0-9]+-[1-9]*[0-9]*")) {
            int index_0 = range.indexOf('=') + 1, index_1 = range.indexOf('-');
            start = Long.parseLong(range.substring(index_0, index_1));
            end = '-' == range.charAt(range.length() - 1) ? fileLen - 1 : Long.parseLong(range.substring(index_1 + 1));
        } else {
            start = 0;
            end = fileLen - 1;
        }
        transLen = end - start + 1;
        HttpResponse response = new DefaultHttpResponse(HTTP_1_1, OK);
        HttpUtil.setContentLength(response, transLen);
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/octet-stream");
        response.headers().set(HttpHeaderNames.CONTENT_DISPOSITION, "attachment;filename=DownloadFile");
        response.headers().set(HttpHeaderNames.CONTENT_RANGE, "bytes " + start + "-" + end + "/" + fileLen);
        response.headers().set("st", "2");
        response.headers().set("fn", encodeFileName(file.getName()));
        response.headers().set("md", md5);
        setDateAndCacheHeaders(response, file);
        // Write the initial line and the header.
        ctx.write(response);
        // Write the content.
        ChannelFuture sendFileFuture;
        ChannelFuture lastContentFuture;
        if (start >= fileAcc.length()) {
            lastContentFuture = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
        } else if (ctx.pipeline().get(SslHandler.class) == null) {
            sendFileFuture =
                    ctx.write(new DefaultFileRegion(fileAcc.getChannel(), start, transLen), ctx.newProgressivePromise());
            // Write the end marker.
            lastContentFuture = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
            sendFileFuture.addListener(channelProgressiveFutureListener);
        } else {
            sendFileFuture =
                    ctx.writeAndFlush(new HttpChunkedInput(new ChunkedFile(fileAcc, start, transLen, 8192)),
                            ctx.newProgressivePromise());
            // HttpChunkedInput will write the end marker (LastHttpContent) for us.
            lastContentFuture = sendFileFuture;
            sendFileFuture.addListener(channelProgressiveFutureListener);
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

    private static Record getNextRecord(int currVer) {
        Record record = null;
        int nextVer = currVer;
        int serverVer = Synchronizer.getCurrVer();
        while (nextVer < serverVer) {
            record = Synchronizer.getRecord(null, ++nextVer, null, null, null);
            if (record != null
                    && record.getState() != SyncState.LOSE
                    && record.getState() != SyncState.DOWNLOADED
                    && (new File(ConfFactory.get("videoPath"), record.getFileName())).exists()) break;
        }
        return record;
    }

    private static FullHttpResponse createNextFileResp(Record nextRecord, String status) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, ByteBuffUtils.empty());
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
        response.headers().set("st", status);
        response.headers().set("vs", Integer.toString(nextRecord.getVersion()));
        response.headers().set("fn", encodeFileName(nextRecord.getFileName()));
        HttpUtil.setContentLength(response, 0);
        return response;
    }


    private static final String HTTP_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss zzz";
    private static final String HTTP_DATE_GMT_TIMEZONE = "GMT";
    private static final int HTTP_CACHE_SECONDS = 60;

    /**
     * Sets the Date and Cache headers for the HTTP Response
     *
     * @param response    HTTP response
     * @param fileToCache file to extract content type
     */
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

    private static String encodeFileName(String fileName) {
        String encode = "";
        try {
            encode = URLEncoder.encode(fileName, "utf8");
        } catch (UnsupportedEncodingException e) {
            log.error("文件名编码异常！", e);
        }
        return encode;
    }
}
