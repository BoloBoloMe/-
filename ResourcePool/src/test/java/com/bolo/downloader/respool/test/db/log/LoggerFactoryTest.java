package com.bolo.downloader.respool.test.db.log;

import com.bolo.downloader.respool.log.LoggerFactory;
import com.bolo.downloader.respool.log.MyLogger;

public class LoggerFactoryTest {
    public static void main(String[] args) {
        LoggerFactory.setLogPath("D:\\MyResource\\Desktop\\log\\");
        LoggerFactory.setLogFileName("test.log");
        LoggerFactory.roll();
        MyLogger log = LoggerFactory.getLogger(LoggerFactoryTest.class);
        log.info("info 参数：%s ，%s", "小明", "1");
        log.info("info 无参数");
        try {
            throw new RuntimeException("2333333333333");
        } catch (Exception e) {
            log.error("error 传递异常：", e);
            log.error("error 无参数");
        }
    }
}
