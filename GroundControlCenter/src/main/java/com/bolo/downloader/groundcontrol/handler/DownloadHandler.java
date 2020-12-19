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
import java.security.NoSuchAlgorithmException;

public class DownloadHandler extends AbstractResponseHandler {
    private MyLogger log = LoggerFactory.getLogger(DownloadHandler.class);

    @Override
    boolean interested(int responseStatus) {
        return 2 == responseStatus;
    }

    @Override
    HttpRequestBase handleResponse(Response response) {
        log.info("[transfer] 开始文件传送.name=", response.getFileNane());
        StoneMap map = StoneMapFactory.getObject();
        int lastVer = Integer.parseInt(map.get(StoneMapDict.KEY_LAST_VER));
        File tar = new File(ConfFactory.get("filePath"), response.getFileNane());
        if (!tar.exists()) {
            try {
                tar.createNewFile();
            } catch (IOException e) {
                log.error("[transfer] 文件创建失败！name=" + response.getFileNane(), e);
                return post(lastVer, 1);
            }
        }
        try (BufferedInputStream inputStream = new BufferedInputStream(response.getEntity().getContent(), 8192);
             BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(tar, true))) {
            // 跳过已下载的字节
            long pass = 0, skip = tar.length();
            while (pass < skip) pass += inputStream.read();
            // 下载文件
            byte[] buff = new byte[8192];
            int len;
            while (0 < (len = inputStream.read(buff))) {
                outputStream.write(buff, 0, len);
            }
            outputStream.flush();
            if (tar.length() == response.getContentLength()) {
                // 文件下载完毕,校验文件完整性
                if (checkMD5(tar, response.getMd5())) {
                    log.info("[transfer] 文件传送完毕,name=%s", response.getFileNane());
                    map.put(StoneMapDict.KEY_FILE_STATE, StoneMapDict.VAL_FILE_STATE_DOWNLOAD);
                    map.flushWriteBuff();
                    return post(lastVer, -1);
                } else {
                    log.error("[transfer] 文件数据不正确，删除文件并重新下载！name=" + response.getFileNane());
                    if (tar.exists()) tar.delete();
                }
            }
        } catch (Exception e) {
            log.error("[transfer] 写入文件数据时发生异常！ name=" + response.getFileNane(), e);
        }
        log.error("[transfer] 传送中止! 传送即将重新开始.name=" + response.getFileNane());
        // 走到这里说吗文件下载最终未成功，继续下载当前文件
        return post(lastVer, 1);
    }

    private boolean checkMD5(File file, String md5) throws IOException, NoSuchAlgorithmException {
        RandomAccessFile target = new RandomAccessFile(file, "r");
        String tarMD5 = MD5Util.md5HashCode32(target);
        return md5.equals(tarMD5);
    }
}
