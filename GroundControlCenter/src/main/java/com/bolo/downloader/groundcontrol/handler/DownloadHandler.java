package com.bolo.downloader.groundcontrol.handler;

import com.bolo.downloader.groundcontrol.dict.StoneMapDict;
import com.bolo.downloader.groundcontrol.factory.ConfFactory;
import com.bolo.downloader.groundcontrol.factory.StoneMapFactory;
import com.bolo.downloader.respool.coder.MD5Util;
import com.bolo.downloader.respool.db.StoneMap;
import com.bolo.downloader.respool.log.LoggerFactory;
import com.bolo.downloader.respool.log.MyLogger;
import org.apache.http.client.methods.HttpRequestBase;

import java.io.*;
import java.net.SocketTimeoutException;
import java.security.NoSuchAlgorithmException;

public class DownloadHandler extends AbstractResponseHandler {
    private MyLogger log = LoggerFactory.getLogger(DownloadHandler.class);
    private final int bufferSize = 8192;

    @Override
    boolean interested(int responseStatus) {
        return 2 == responseStatus;
    }

    @Override
    HttpRequestBase handleResponse(Response response) {
        log.info("[transfer] 开始文件传送.name=%s", response.getFileNane());
        StoneMap map = StoneMapFactory.getObject();
        int lastVer = Integer.parseInt(map.get(StoneMapDict.KEY_LAST_VER));
        File tar = new File(ConfFactory.get("downloadDir"), response.getFileNane());
        if (!tar.exists()) {
            try {
                tar.createNewFile();
            } catch (IOException e) {
                log.error("[transfer] 文件创建失败！name=" + response.getFileNane(), e);
                return post(lastVer, 1, 0);
            }
        }
        // 下载文件
        boolean catchTimeout = false;
        try (BufferedInputStream inputStream = new BufferedInputStream(response.getEntity().getContent(), bufferSize);
             BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(tar, true), bufferSize)) {
            byte[] buff = new byte[bufferSize];
            int len;
            for (int num = 1; 0 < (len = inputStream.read(buff)); num++) {
                outputStream.write(buff, 0, len);
                if (num % 100 == 0) {
                    outputStream.flush();
                }
            }
            outputStream.flush();
            log.info("[transfer] 文件传送结束.");
        } catch (SocketTimeoutException e) {
            log.info("[transfer] SocketTimeout!", e);
            catchTimeout = true;
        } catch (Exception e) {
            log.error("[transfer] 文件传送时发生异常！ name=" + response.getFileNane(), e);
        }
        // 校验文件
        long fileLen = tar.length();
        try {
            if (fileLen == response.getFileSize()) {
                log.info("[transfer] 接收的数据已达文件大小,开始校验数据的完整性");
                if (checkMD5(tar, response.getMd5())) {
                    log.info("[transfer] 文件完整性校验通过,传输已完成. name=%s", response.getFileNane());
                    map.put(StoneMapDict.KEY_FILE_STATE, StoneMapDict.VAL_FILE_STATE_DOWNLOAD);
                    map.flushWriteBuff();
                    return post(lastVer, -1, 0);
                } else {
                    log.error("[transfer] 文件数据不正确，删除文件并重新下载！name=" + response.getFileNane());
                    if (tar.exists()) tar.delete();
                    return post(lastVer, 1, 0);
                }
            } else {
                log.info("[transfer] 文件数据尚未传送完整,重新请求文件数据.");
                // 如果请求文件数据时捕获了超时，就等待服务器一段时间再重新发送请求，且下次请求的超时时间设置为1分钟
                if (catchTimeout) {
                    log.info("[transfer] 因发生SocketTimeout, 文件将重新下载.");
                    if (tar.exists()) tar.delete();
                    return post(lastVer, 1, 0);
                } else {
                    return post(lastVer, 1, fileLen);
                }
            }
        } catch (Exception e) {
            log.error("[transfer] 数据完整性校验异常！", e);
        }
        return post(lastVer, 1, fileLen);
    }

    private boolean checkMD5(File file, String md5) throws IOException, NoSuchAlgorithmException {
        RandomAccessFile target = new RandomAccessFile(file, "r");
        String tarMD5 = MD5Util.md5HashCode32(target);
        return md5.equals(tarMD5);
    }
}
