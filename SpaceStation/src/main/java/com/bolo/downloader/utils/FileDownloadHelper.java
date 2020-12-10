package com.bolo.downloader.utils;

import com.bolo.downloader.factory.ConfFactory;
import com.bolo.downloader.respool.log.LoggerFactory;
import com.bolo.downloader.respool.log.MyLogger;
import com.bolo.downloader.sync.Record;
import com.bolo.downloader.sync.SyncState;
import com.bolo.downloader.sync.Synchronizer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;

import java.io.File;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;

public class FileDownloadHelper {
    private static final MyLogger log = LoggerFactory.getLogger(FileDownloadHelper.class);
    private static final FullHttpResponse equalsResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, ByteBuffUtils.empty());


    static {
        // init equals response
        equalsResponse.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
        equalsResponse.headers().set("st", "0");
        equalsResponse.headers().set("sp", "0");
        equalsResponse.headers().set("vs", "0");
        equalsResponse.headers().set("fn", "");
        HttpUtil.setContentLength(equalsResponse, 0);
    }

    public static boolean handle(String uri, Map<String, List<String>> params, ChannelHandlerContext ctx, FullHttpRequest request) {
        int serverVer = Synchronizer.getCurrVer();
        // get params
        List<String> cvs = params.get("cv");
        List<String> sps = params.get("sp");
        List<String> els = params.get("el");
        Integer clientVer = cvs == null || cvs.size() == 0 ? null : Integer.valueOf(cvs.get(0));
        Long skip = sps == null || sps.size() == 0 ? null : Long.valueOf(sps.get(0));
        Integer expectedLen = els == null || els.size() == 0 ? null : Integer.valueOf(els.get(0));
        if (skip == null || clientVer == null) {
            ResponseHelper.sendError(ctx, HttpResponseStatus.BAD_REQUEST, request);
            return false;
        }
        if (serverVer < clientVer) {
            ResponseHelper.sendAndCleanupConnection(ctx, request, equalsResponse, false);
            return false;
        }
        // serverVer >= clientVer
        File tar = null;
        Record record = null;
        int exampleVer = clientVer;
        while (exampleVer <= serverVer) {
            record = Synchronizer.getRecord(null, exampleVer++, null, null, null);
            if (record != null
                    && record.getState() != SyncState.LOSE
                    && record.getState() != SyncState.DOWNLOADED
                    && (tar = new File(ConfFactory.get("videoPath"), record.getFileName())).exists()) break;
        }
        // 如果客户端版本号之后的所有版本都找不到文件，仍然返回 equalsResponse, 客户端仍然能继续正常运行
        if (record == null) {
            ResponseHelper.sendAndCleanupConnection(ctx, request, equalsResponse, false);
            return false;
        }
        if (record.getVersion() == clientVer) {
            // 下载当前请求的文件
            try (RandomAccessFile fileAcc = new RandomAccessFile(tar, "r")) {
                if (skip >= fileAcc.length()) {
                    // 请求的文件已经同步至末尾，尝试获取下一个文件
                    Synchronizer.isDownloaded(record.getFileName());
                    record = null;
                    exampleVer = clientVer + 1;
                    while (exampleVer <= serverVer) {
                        record = Synchronizer.getRecord(null, exampleVer++, null, null, null);
                        if (record != null && record.getState() != SyncState.LOSE && new File(ConfFactory.get("videoPath"), record.getFileName()).exists())
                            break;
                    }
                    if (record == null) {
                        ResponseHelper.sendAndCleanupConnection(ctx, request, equalsResponse, false);
                    } else {
                        ResponseHelper.sendAndCleanupConnection(ctx, request, createNextFileResp(record, "1"), false);
                    }
                    return false;
                }
                fileAcc.seek(skip);
                // 实际响应的文件内容长度取决于请求的长度
                int conLen = null == expectedLen || expectedLen < 0 ? 1024 : expectedLen;
                byte[] con = new byte[conLen];
                int realLen = fileAcc.read(con, 0, con.length);
                ByteBuf content = ByteBuffUtils.copy(con, realLen);
                ResponseHelper.sendAndCleanupConnection(ctx, request, createFileWriteResp(skip, record.getVersion(), content, realLen), false);
            } catch (Exception e) {
                log.error("服务器文件读取失败！", e);
                ResponseHelper.sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, request);
            }
            return false;
        } else {
            // 告诉客户端请求的文件已丢失，并返回下一个版本的文件信息
            try {
                ResponseHelper.sendAndCleanupConnection(ctx, request, createNextFileResp(record, "3"), false);
            } catch (UnsupportedEncodingException e) {
                log.error("文件名编码失败！", e);
                ResponseHelper.sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, request);
            }
            return false;
        }
    }

    private static FullHttpResponse createNextFileResp(Record nextRecord, String status) throws UnsupportedEncodingException {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, ByteBuffUtils.empty());
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
        response.headers().set("st", status);
        response.headers().set("sp", "0");
        response.headers().set("vs", Integer.toString(nextRecord.getVersion()));
        response.headers().set("fn", URLEncoder.encode(nextRecord.getFileName(), "utf8"));
        HttpUtil.setContentLength(response, 0);
        return response;
    }

    private static FullHttpResponse createFileWriteResp(long spik, int version, ByteBuf content, int len) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, content);
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
        response.headers().set("st", "2");
        response.headers().set("sp", Long.toString(spik));
        response.headers().set("vs", Integer.toString(version));
        response.headers().set("fn", "");
        HttpUtil.setContentLength(response, len);
        return response;
    }


}
