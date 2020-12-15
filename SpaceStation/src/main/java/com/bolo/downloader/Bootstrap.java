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

import java.util.concurrent.BlockingDeque;


public class Bootstrap {
    private static final String CONF_FILE_PATH = "conf/SpaceStation.conf";
    private static final BlockingDeque<ReqRecord> deque = ReqQueueFactory.get();
    private static MyLogger log = LoggerFactory.getLogger(Bootstrap.class);
    private static StoneMap stoneMap;
    private static HttpServer httpServer;

    public static void main(String[] args) {
        // load configure
        ConfFactory.load(CONF_FILE_PATH);
        // init logger
        LoggerFactory.setLogPath(ConfFactory.get("logPath"));
        LoggerFactory.setLogFileName(ConfFactory.get("logFileName"));
        LoggerFactory.roll();
        // load stone map and cache file list
        stoneMap = StoneMapFactory.getObject();
        Synchronizer.cache(stoneMap);
        log.info("服务端当前版本号：%s", Integer.toString(Synchronizer.getCurrVer()));
        // start httpServer
        httpServer = new HttpServer(Integer.parseInt(ConfFactory.get("port")), false);
        try {
            httpServer.start();
        } catch (Exception e) {
            throw new Error("服务器启动失败", e);
        }
    }


    public static void shutdownGracefully() {
        httpServer.shutdown();
        DownloaderFactory.getObject().shudown();
        Synchronizer.shudown();
        stoneMap.rewriteDbFile();
        log.info("进程已结束！");
    }
}

