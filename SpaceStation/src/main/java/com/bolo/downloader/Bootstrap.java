package com.bolo.downloader;


import com.bolo.downloader.factory.ConfFactory;
import com.bolo.downloader.factory.DownloaderFactory;
import com.bolo.downloader.factory.ReqQueueFactory;
import com.bolo.downloader.factory.StoneMapFactory;
import com.bolo.downloader.nio.HttpServer;
import com.bolo.downloader.nio.ReqRecord;
import com.bolo.downloader.respool.db.StoneMap;
import com.bolo.downloader.respool.log.LoggerFactory;
import com.bolo.downloader.respool.log.MyLogger;
import com.bolo.downloader.sync.Synchronizer;
import com.bolo.downloader.utils.PostHelper;
import io.netty.handler.codec.http.HttpUtil;

import javax.net.ssl.SSLException;
import java.security.cert.CertificateException;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.TimeUnit;


public class Bootstrap {
    public static final String CONF_FILE_PATH = "D:\\MyResource\\Desktop\\conf\\SpaceStation.conf";
    //    public static final String CONF_FILE_PATH = "";
    private static int PORT;
    private static final BlockingDeque<ReqRecord> deque = ReqQueueFactory.get();
    private static MyLogger log = LoggerFactory.getLogger(Bootstrap.class);

    static {
        LoggerFactory.setLogPath(ConfFactory.get("logPath"));
        LoggerFactory.setLogFileName(ConfFactory.get("logFileName"));
        LoggerFactory.roll();
    }

    public static void main(String[] args) throws CertificateException, InterruptedException, SSLException {
        // load stone map and cache file list
        StoneMap stoneMap = StoneMapFactory.getObject();
        Synchronizer.cache(stoneMap);
        log.info("服务端当前版本号：%s", Integer.toString(Synchronizer.getCurrVer()));
        // start server
        PORT = Integer.valueOf(ConfFactory.get("port"));
        HttpServer server = new HttpServer(PORT);
        server.start();
        long lastScanDiscTime = 0;
        for (long time = 1; ; time++) {
            // write request loop
            while (true) {
                ReqRecord reqRecord;
                try {
                    // 对于来自同一个连接的请求（比如当客户端使用连接池时），会有不同的请求对象，但这些对象关联的连接是同一个
                    reqRecord = deque.pollLast(1, TimeUnit.SECONDS);
                    if (reqRecord == null || !reqRecord.getCtx().channel().isOpen()) break;
                } catch (InterruptedException e) {
                    break;
                }
                if ("/ssd".equals(reqRecord.getUri())) {
                    try {
                        Thread.sleep(500);
                    } catch (Exception e) {
                    }
                    Synchronizer.scanDisc();
                    Synchronizer.clean();
                    stoneMap.rewriteDbFile();
                    server.shutdown();
                    DownloaderFactory.getObject().shudown();
                    log.info("进程已结束！");
                    return;
                }
                handelReqRecord(reqRecord);
                if (reqRecord.isDone() || !HttpUtil.isKeepAlive(reqRecord.getRequest())) {
                    closeChannel(reqRecord);
                }
            }
            // background loop
            try {
                LoggerFactory.roll();
                if (time - lastScanDiscTime > 5) {
                    lastScanDiscTime = time;
                    Synchronizer.scanDisc();
                    Synchronizer.clean();
                }
                if (stoneMap.modify() < 200) {
                    stoneMap.flushWriteBuff();
                } else {
                    stoneMap.rewriteDbFile();
                }
            } catch (Exception e) {
                log.error("background loop throws exception!", e);
            }
        }
    }

    private static void handelReqRecord(ReqRecord reqRecord) {
        reqRecord.setDone(PostHelper.doPOST(reqRecord.getUri(), reqRecord.getParams(), reqRecord.getCtx(), reqRecord.getRequest()));
    }

    private static void closeChannel(ReqRecord reqRecord) {
        try {
            if (reqRecord.getCtx().channel().isOpen()) {
                reqRecord.getCtx().close();
            }
        } catch (Exception e) {
            log.error("channel close throws exception!", e);
        }
    }
}

