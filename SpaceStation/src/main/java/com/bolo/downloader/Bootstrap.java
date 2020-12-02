package com.bolo.downloader;


import com.bolo.downloader.factory.ConfFactory;
import com.bolo.downloader.factory.StoneMapFactory;
import com.bolo.downloader.net.HttpServer;
import com.bolo.downloader.respool.db.StoneMap;


public class Bootstrap {
    private static int PORT;

    public static void main(String[] args) throws Exception {
        PORT = Integer.valueOf(ConfFactory.get("port"));
        new HttpServer(PORT).start();
        System.out.println("服务启动成功,地址：127.0.0.1:" + PORT);

        // main thread loop
        while (true) {
            // 每次循环都尝试刷新需要持久化的内容
            StoneMap stoneMap = StoneMapFactory.getObject();
            try {
                stoneMap.flushWriteBuff();
            } catch (Exception e) {
                e.printStackTrace();
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}
