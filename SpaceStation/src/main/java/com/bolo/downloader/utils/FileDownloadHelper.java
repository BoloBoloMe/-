package com.bolo.downloader.utils;

import com.bolo.downloader.factory.ConfFactory;
import com.bolo.downloader.respool.coder.MD5Util;
import com.bolo.downloader.respool.coder.RSAUtils;
import com.bolo.downloader.respool.log.LoggerFactory;
import com.bolo.downloader.respool.log.MyLogger;
import com.bolo.downloader.sync.Record;
import com.bolo.downloader.sync.SyncState;
import com.bolo.downloader.sync.Synchronizer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.List;
import java.util.Map;

public class FileDownloadHelper {
    private static final MyLogger log = LoggerFactory.getLogger(FileDownloadHelper.class);
    private static final FullHttpResponse equalsResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, ByteBuffUtils.empty());
    private static final int MAX_CONTENT_LENGTH = 65536; // 64k
    private static final byte[] contentArr = new byte[MAX_CONTENT_LENGTH];
    private static final Charset charset = Charset.forName("utf8");

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
        List<String> pcs = params.get("pc");
        List<String> kis = params.get("ki");
        Integer clientVer = cvs == null || cvs.size() == 0 ? null : Integer.valueOf(cvs.get(0));
        Long skip = sps == null || sps.size() == 0 ? null : Long.valueOf(sps.get(0));
        int expectedLen = els == null || els.size() == 0 ? 1024 : Integer.parseInt(els.get(0));
        String keyId = (kis == null || kis.size() == 0) ? null : kis.get(0);
        RSAPublicKey publicKey = null;
        if (null != pcs && pcs.size() > 0) {
            keyId = ctx.channel().id().asLongText();
            String pc = pcs.get(0);
            publicKey = KeyUtils.add(keyId, pc);
        } else if (keyId != null) {
            publicKey = KeyUtils.get(keyId);
            if (publicKey == null) {
                ResponseHelper.sendAndCleanupConnection(ctx, request, createKeyIdInvalidResp(), false);
            }
        }
        if (skip == null || clientVer == null) {
            ResponseHelper.sendError(ctx, HttpResponseStatus.BAD_REQUEST, request);
            return false;
        }
        if (serverVer < clientVer) {
            ResponseHelper.sendAndCleanupConnection(ctx, request, equalsResponse, false);
            return false;
        }
        // serverVer >= clientVer
        if (expectedLen <= 0) {
            // 文件已经同步完毕,更新当前文件记录的状态
            Record currRecord = Synchronizer.getRecord(null, clientVer, null, null, null);
            if (null != currRecord) Synchronizer.isDownloaded(currRecord.getFileName());
            // 请求下一个需要同步的文件
            Record nextRecord = getNextRecord(clientVer);
            // 如果客户端版本号之后的所有版本都找不到文件,返回 equalsResponse
            if (nextRecord == null) {
                ResponseHelper.sendAndCleanupConnection(ctx, request, equalsResponse, false);
            } else {
                ResponseHelper.sendAndCleanupConnection(ctx, request, createNextFileResp(nextRecord, "1", publicKey, keyId), false);
            }
            return false;
        }
        // expectedLen > 0 下载文件
        Record record = Synchronizer.getRecord(null, clientVer, null, null, null);
        if (record == null || SyncState.LOSE.equals(record.getState())) {
            // 要下载的文件已丢失，尝试寻找下一个可下载的文件
            record = getNextRecord(clientVer);
            if (record == null) {
                // 暂无需要下载的文件,返回 equalsResponse
                ResponseHelper.sendAndCleanupConnection(ctx, request, equalsResponse, false);
            } else {
                ResponseHelper.sendAndCleanupConnection(ctx, request, createNextFileResp(record, "3", publicKey, keyId), false);
            }
            return false;
        }
        // 要下载的文件还在，下载当前文件
        ByteBuf content;
        try (RandomAccessFile fileAcc = new RandomAccessFile(ConfFactory.get("videoPath") + File.separator + record.getFileName(), "r")) {
            long fileLen;
            if (skip >= (fileLen = fileAcc.length())) {
                // 文件已读至末尾
                ResponseHelper.sendAndCleanupConnection(ctx, request, createFileEndResp(record.getVersion(), fileLen, getFileMD5(fileAcc)), false);
                return false;
            }
            fileAcc.seek(skip);
            // 实际响获取的长度要考虑文件实际长度和服务器的内存限制
            int conLen = expectedLen;
            if (expectedLen > MAX_CONTENT_LENGTH) {
                conLen = MAX_CONTENT_LENGTH;
            }
            int realLen = fileAcc.read(contentArr, 0, conLen);
            byte[] writeBuff;
            if (publicKey == null || "".equals(publicKey)) {
                writeBuff = contentArr;
            } else {
                writeBuff = RSAUtils.encode(contentArr, realLen, publicKey);
                realLen = writeBuff.length;
            }
            content = ByteBuffUtils.bigBuff();
            content.writeBytes(writeBuff, 0, realLen);
            ResponseHelper.sendAndCleanupConnection(ctx, request, createFileWriteResp(skip, record.getVersion(), content, realLen, keyId, ""), false);
        } catch (Exception e) {
            log.error("服务器文件读取失败！", e);
            ResponseHelper.sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, request);
        }
        return false;
    }

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

    private static FullHttpResponse createNextFileResp(Record nextRecord, String status, RSAPublicKey key, String keyId) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, ByteBuffUtils.empty());
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
        response.headers().set("st", status);
        response.headers().set("sp", "0");
        response.headers().set("vs", Integer.toString(nextRecord.getVersion()));
        if (keyId != null) response.headers().set("ki", keyId);
        try {
            if (key == null) {
                response.headers().set("fn", URLEncoder.encode(nextRecord.getFileName(), "utf8"));
            } else {
                byte[] fileName = nextRecord.getFileName().getBytes(charset);
                String base64Name = Base64.getEncoder().encodeToString(RSAUtils.encode(fileName, fileName.length, key));
                String urlEncodeName = URLEncoder.encode(base64Name, "utf8");
                response.headers().set("fn", urlEncodeName);
            }
        } catch (Exception e) {
            log.error("文件名编码失败！", e);
        }
        HttpUtil.setContentLength(response, 0);
        return response;
    }

    private static FullHttpResponse createFileWriteResp(long spik, int version, ByteBuf content, int len, String keyId, String md5) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, content);
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
        response.headers().set("st", "2");
        response.headers().set("sp", Long.toString(spik));
        response.headers().set("vs", Integer.toString(version));
        response.headers().set("fn", md5);
        if (keyId != null) response.headers().set("ki", keyId);
        HttpUtil.setContentLength(response, len);
        return response;
    }

    private static FullHttpResponse createFileEndResp(int version, long skip, String md5) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, ByteBuffUtils.empty());
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
        response.headers().set("st", "4");
        response.headers().set("sp", skip);
        response.headers().set("vs", Integer.toString(version));
        response.headers().set("fn", md5);
        HttpUtil.setContentLength(response, 0);
        return response;
    }

    private static FullHttpResponse createKeyIdInvalidResp() {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, ByteBuffUtils.empty());
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
        response.headers().set("st", "5");
        response.headers().set("sp", "0");
        response.headers().set("vs", "0");
        response.headers().set("fn", "");
        HttpUtil.setContentLength(response, 0);
        return response;
    }

    private static String getFileMD5(RandomAccessFile file) throws IOException, NoSuchAlgorithmException {
        return MD5Util.md5HashCode32(file);
    }

    private static String getFileMD5(RandomAccessFile file, long len) throws IOException, NoSuchAlgorithmException {
        return MD5Util.md5HashCode32(file, len);
    }


}
