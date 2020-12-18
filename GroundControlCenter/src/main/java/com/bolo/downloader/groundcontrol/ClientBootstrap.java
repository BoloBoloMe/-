package com.bolo.downloader.groundcontrol;


import com.bolo.downloader.groundcontrol.dict.StoneMapDict;
import com.bolo.downloader.groundcontrol.factory.ConfFactory;
import com.bolo.downloader.groundcontrol.factory.HttpClientFactory;
import com.bolo.downloader.groundcontrol.factory.StoneMapFactory;
import com.bolo.downloader.groundcontrol.handler.AbstractResponseHandler;
import com.bolo.downloader.groundcontrol.handler.DownloadHandler;
import com.bolo.downloader.groundcontrol.handler.EqualsHandler;
import com.bolo.downloader.groundcontrol.handler.NewFileHandler;
import com.bolo.downloader.respool.db.StoneMap;
import com.bolo.downloader.respool.log.LoggerFactory;
import com.bolo.downloader.respool.log.MyLogger;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.impl.client.CloseableHttpClient;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;

public class ClientBootstrap {
    private static final String CONF_FILE_PATH = "conf/GroundControlCenter.conf";
    static private final MyLogger log = LoggerFactory.getLogger(ClientBootstrap.class);


    public static void main(String[] args) {
        init();
        // StoneMap键值对关系：lastVerKey->lastVer; lastVer->lastFileName; lastFileName-> isDone
        final StoneMap map = StoneMapFactory.getObject();
        int expectedValue, lastVer;
        if (null != map.get(StoneMapDict.KEY_LAST_VER) && null != map.get(StoneMapDict.KEY_LAST_FILE) && null != map.get(StoneMapDict.KEY_FILE_STATE)) {
            // StoneMap 保存着进程关闭前的状态，进行读取
            lastVer = Integer.parseInt(map.get(StoneMapDict.KEY_LAST_VER));
            if (StoneMapDict.VAL_FILE_STATE_DOWNLOAD.equals(map.get(StoneMapDict.KEY_FILE_STATE))) {
                // 上次关闭前下载的文件已经完成下载，通知服务器清除文件
                expectedValue = -1;
            } else {
                // 上次关闭前下载的文件尚未完成下载，向服务器请求文件内容
                expectedValue = 1;
            }
        } else {
            // StoneMap 中没有保存有效信息，初始化各变量
            lastVer = 0;
            expectedValue = 1;
            map.clear();
        }
        final CloseableHttpClient client = HttpClientFactory.http();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                client.close();
                StoneMapFactory.getObject().rewriteDbFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }));
        AbstractResponseHandler parentHandler = new EqualsHandler().join(new NewFileHandler()).join(new DownloadHandler());
        log.info("客户端启动成功.");
        log.info("当前版本号: %d", lastVer);
        log.info("访问的服务器地址: %s", ConfFactory.get("url"));
        HttpUriRequest request = AbstractResponseHandler.post(lastVer, expectedValue);
        while (!Thread.interrupted()) {
            try {
                request = client.execute(request, parentHandler);
            } catch (HttpHostConnectException e) {
                log.error("服务器无法访问！", e);
                sleep(10000);
            } catch (IOException e) {
                log.error("捕获异常！", e);
            }
        }
    }


    private static void init() {
        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
        String name = runtime.getName();
        System.out.println("当前进程： " + name);
        log.info("当前进程： " + name);
        ConfFactory.load(CONF_FILE_PATH);
        LoggerFactory.setLogPath(ConfFactory.get("logPath"));
        LoggerFactory.setLogFileName(ConfFactory.get("logFileName"));
        LoggerFactory.roll();
    }

    private static void sleep(long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
        }
    }
}
