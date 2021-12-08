package com.bolo.downloader.groundcontrol.file.synchronizer;


import com.bolo.downloader.groundcontrol.constant.StoneMapDict;
import com.bolo.downloader.groundcontrol.factory.ConfFactory;
import com.bolo.downloader.groundcontrol.factory.HttpClientFactory;
import com.bolo.downloader.groundcontrol.factory.StoneMapFactory;
import com.bolo.downloader.groundcontrol.util.MainPool;
import com.bolo.downloader.respool.db.StoneMap;
import com.bolo.downloader.respool.log.LoggerFactory;
import com.bolo.downloader.respool.log.MyLogger;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.impl.client.CloseableHttpClient;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class Synchronizer {
    private static final MyLogger log = LoggerFactory.getLogger(Synchronizer.class);
    private static final ResponseHandler<HttpRequestBase> HANDLERS = new EqualsHandler().join(new NewFileHandler()).join(new DownloadHandler());
    private static final CloseableHttpClient client = HttpClientFactory.http();
    private static final AtomicReference<HttpRequestBase> NEXT_REQ = new AtomicReference<>(null);


    public static void run() {
        if (!Boolean.TRUE.toString().equals(ConfFactory.get("openSyncTask"))) {
            return;
        }
        log.info("文件同步任务已开启，远程服务器地址: %s", ConfFactory.get("url"));
        executeRequestAsync(0, TimeUnit.MILLISECONDS);
    }


    private static void executeRequestAsync(long delay, TimeUnit unit) {
        MainPool.executor().schedule(() -> {
            HttpRequestBase thisReq = NEXT_REQ.get();
            if (Objects.isNull(thisReq)) {
                thisReq = createRequestFromStone();
            } else {
                NEXT_REQ.compareAndSet(thisReq, null);
            }
            boolean errorFlag;
            try {
                HttpRequestBase nextRequest = client.execute(thisReq, HANDLERS);
                NEXT_REQ.compareAndSet(null, nextRequest);
                errorFlag = true;
            } catch (HttpHostConnectException e) {
                log.error("服务器无法访问！", e);
                errorFlag = false;
            } catch (IOException e) {
                log.error("捕获异常！", e);
                errorFlag = false;
            }
            if (errorFlag) {
                executeRequestAsync(10, TimeUnit.MINUTES);
            } else {
                executeRequestAsync(3, TimeUnit.MINUTES);
            }
        }, delay, unit);
    }


    /**
     * 根据StoneMap中的信息构建请求对象
     */
    private static HttpRequestBase createRequestFromStone() {
        final StoneMap map = StoneMapFactory.getObject();
        int expectedValue, lastVer;
        long skip;
        if (null != map.get(StoneMapDict.KEY_LAST_VER) && null != map.get(StoneMapDict.KEY_LAST_FILE) && null != map.get(StoneMapDict.KEY_FILE_STATE)) {
            // StoneMap 保存着进程关闭前的状态，进行读取
            lastVer = Integer.parseInt(map.get(StoneMapDict.KEY_LAST_VER));
            if (StoneMapDict.VAL_FILE_STATE_DOWNLOAD.equals(map.get(StoneMapDict.KEY_FILE_STATE))) {
                // 上次关闭前下载的文件已经完成下载，通知服务器清除文件
                expectedValue = -1;
                skip = 0;
            } else {
                // 上次关闭前下载的文件尚未完成下载，向服务器请求文件内容
                expectedValue = 1;
                File target = new File(ConfFactory.get("filePath"), map.get(StoneMapDict.KEY_LAST_FILE));
                if (target.exists()) {
                    skip = target.length();
                } else {
                    skip = 0;
                }
            }
        } else {
            // StoneMap 中没有保存有效信息，初始化各变量
            lastVer = 0;
            expectedValue = 1;
            skip = 0;
            map.clear();
        }
        log.info("根据StoneMap构建请求: version=%d,expectedValue=%d,skip=%d", lastVer, expectedValue, skip);
        return AbstractResponseHandler.post(lastVer, expectedValue, skip);
    }
}
