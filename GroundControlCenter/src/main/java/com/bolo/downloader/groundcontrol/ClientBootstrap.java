package com.bolo.downloader.groundcontrol;

import com.bolo.downloader.groundcontrol.factory.ConfFactory;
import com.bolo.downloader.groundcontrol.factory.StoneMapFactory;
import com.bolo.downloader.groundcontrol.file.synchronizer.Synchronizer;
import com.bolo.downloader.groundcontrol.server.NetServer;
import com.bolo.downloader.groundcontrol.util.FileMap;
import com.bolo.downloader.groundcontrol.util.MainPool;
import com.bolo.downloader.respool.log.LoggerFactory;
import com.bolo.downloader.respool.log.MyLogger;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.Optional;

public class ClientBootstrap {
    private static final String CONF_FILE_PATH = Optional.ofNullable(System.getProperty("conf.path")).orElse("conf/GroundControlCenter.propertes");
    private static final MyLogger log = LoggerFactory.getLogger(ClientBootstrap.class);

    public static void main(String[] args) {
        init();
        NetServer.start(Integer.parseInt(ConfFactory.get("port")));
        Synchronizer.run();
    }

    public static void shutdownGracefully() {
        new Thread(() -> {
            try {
                StoneMapFactory.getObject().rewriteDbFile();
                NetServer.shutdown(Integer.parseInt(ConfFactory.get("port")));
                MainPool.executor().shutdown();
                System.err.println("系统将在5秒中后停止运行.");
                int countdown = 5;
                while (countdown > 0) {
                    System.err.println(countdown);
                    Thread.sleep(1000);
                    countdown--;
                }
                System.err.println(countdown);
                System.exit(0);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

    }


    private static void init() {
        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
        String name = runtime.getName();
        log.info("当前进程： " + name);
        ConfFactory.load(CONF_FILE_PATH);
        LoggerFactory.setLogPath(ConfFactory.get("logPath"));
        LoggerFactory.setLogFileName(ConfFactory.get("logFileName"));
        LoggerFactory.roll();
        FileMap.flush();
    }

}
