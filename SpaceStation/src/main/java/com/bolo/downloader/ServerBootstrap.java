package com.bolo.downloader;


import com.bolo.downloader.factory.ConfFactory;
import com.bolo.downloader.factory.DownloaderFactory;
import com.bolo.downloader.nio.HttpServer;
import com.bolo.downloader.respool.log.LoggerFactory;
import com.bolo.downloader.respool.log.MyLogger;
import com.bolo.downloader.respool.nio.utils.PageUtil;
import com.bolo.downloader.sync.Synchronizer;

import java.util.Optional;

public class ServerBootstrap {
    private static final String CONF_FILE_PATH = Optional.ofNullable(System.getProperty("conf.path")).orElse("conf/SpaceStation.propertes");
    private static MyLogger log = LoggerFactory.getLogger(ServerBootstrap.class);
    private static HttpServer httpServer;

    public static void main(String[] args) {
        // load configure
        ConfFactory.load(CONF_FILE_PATH);
        PageUtil.setBasic(ConfFactory.get("staticFilePath"));
        // init logger
        LoggerFactory.setLogPath(ConfFactory.get("logPath"));
        LoggerFactory.setLogFileName(ConfFactory.get("logFileName"));
        LoggerFactory.roll();
        Synchronizer.cache();
        Synchronizer.flush();
        log.info("服务端当前版本号：%s", Integer.toString(Synchronizer.getCurrVer()));
        // start httpServer
        httpServer = new HttpServer(Integer.parseInt(ConfFactory.get("port")));
        try {
            httpServer.start();
        } catch (Exception e) {
            throw new Error("服务器启动失败", e);
        }
    }


    public static void shutdownGracefully() {
        httpServer.shutdown();
        DownloaderFactory.getObject().shudown();
        Synchronizer.clean();
        log.info("进程已结束！");
    }
}

