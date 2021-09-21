package com.bolo.downloader.groundcontrol;

import com.bolo.downloader.groundcontrol.dict.StoneMapDict;
import com.bolo.downloader.groundcontrol.factory.ConfFactory;
import com.bolo.downloader.groundcontrol.factory.HttpClientFactory;
import com.bolo.downloader.groundcontrol.factory.StoneMapFactory;
import com.bolo.downloader.groundcontrol.handler.AbstractResponseHandler;
import com.bolo.downloader.groundcontrol.handler.DownloadHandler;
import com.bolo.downloader.groundcontrol.handler.EqualsHandler;
import com.bolo.downloader.groundcontrol.handler.NewFileHandler;
import com.bolo.downloader.groundcontrol.server.NetServer;
import com.bolo.downloader.groundcontrol.util.FileMap;
import com.bolo.downloader.respool.db.StoneMap;
import com.bolo.downloader.respool.log.LoggerFactory;
import com.bolo.downloader.respool.log.MyLogger;
import com.bolo.downloader.respool.nio.utils.PageUtil;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.impl.client.CloseableHttpClient;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class ClientBootstrap {
    private static final String CONF_FILE_PATH = "conf/GroundControlCenter.conf";
    private static final MyLogger log = LoggerFactory.getLogger(ClientBootstrap.class);
    private static NetServer server;
    private static CloseableHttpClient client;
    private static volatile long SYSTEM_TIME_MILLISECOND = System.currentTimeMillis();

    public static void main(String[] args) {
        init();
        client = HttpClientFactory.http();
        server = new NetServer(Integer.parseInt(ConfFactory.get("port")));
        server.start();
        ResponseHandler<HttpRequestBase> handlers = new EqualsHandler().join(new NewFileHandler()).join(new DownloadHandler());
        log.info("Space Station 服务器地址: %s", ConfFactory.get("url"));
        HttpUriRequest request = createRequestFromStone();
        boolean catchConnectException = false;
        while (!Thread.interrupted()) {
            increaseSystemTime();
            try {
                runTask();
                if (Boolean.TRUE.toString().equals(ConfFactory.get("openSyncTask"))) {
                    if (catchConnectException) sleep(10000);
                    request = client.execute(request, handlers);
                }
                continue;
            } catch (HttpHostConnectException e) {
                log.error("服务器无法访问！", e);
                catchConnectException = true;
            } catch (IOException e) {
                log.error("捕获异常！", e);
            }
            request = createRequestFromStone();
        }
    }

    public static void shutdownGracefully() {
        try {
            server.shutdown();
            client.close();
            StoneMapFactory.getObject().rewriteDbFile();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void sleep(long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
        }
    }

    public static long getSystemTime() {
        return SYSTEM_TIME_MILLISECOND;
    }


    private static void init() {
        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
        String name = runtime.getName();
        log.info("当前进程： " + name);
        ConfFactory.load(CONF_FILE_PATH);
        LoggerFactory.setLogPath(ConfFactory.get("logPath"));
        LoggerFactory.setLogFileName(ConfFactory.get("logFileName"));
        LoggerFactory.roll();
        PageUtil.setBasic(ConfFactory.get("staticFilePath"));
        PageUtil.setDynamicPageList("/page/playVideo.html");
        PageUtil.setDynamicPageList("/page/playAudio.html");
        FileMap.flush();
    }

    private static void increaseSystemTime() {
        SYSTEM_TIME_MILLISECOND = System.currentTimeMillis();
    }

    /**
     * 根据StoneMap中的信息构建请求对象
     */
    private static HttpUriRequest createRequestFromStone() {
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

    /**
     * 全局的异步任务队列
     */
    private static final LinkedBlockingQueue<Runnable> taskDeque = new LinkedBlockingQueue<>();

    public static void submitTask(Runnable runnable) {
        taskDeque.add(runnable);
    }

    private static void runTask() {
        try {
            while (!Thread.interrupted()) {
                Runnable task = taskDeque.poll(100, TimeUnit.MILLISECONDS);
                if (null == task) break;
                task.run();
            }
        } catch (InterruptedException e) {

        }
    }

}
