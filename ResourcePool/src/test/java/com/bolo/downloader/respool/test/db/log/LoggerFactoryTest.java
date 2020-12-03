package com.bolo.downloader.respool.test.db.log;

import com.bolo.downloader.respool.log.LoggerFactory;
import com.bolo.downloader.respool.log.MyLogger;

public class LoggerFactoryTest {
    public static void main(String[] args) {
        LoggerFactory.setLogPath("D:\\MyResource\\Desktop\\log\\");
        LoggerFactory.setLogFileName("test.log");
        MyLogger log = LoggerFactory.getLogger();
        log.info("你好，%s！", "小明");
        try {
            throw new RuntimeException("2333333333333");
        } catch (Exception e) {
            log.error("捕获异常！", e);
        }
    }
}
