package com.bolo.downloader.respool.test.db.log;

import java.io.IOException;

import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

import java.util.logging.Logger;

public class TestJDKLog {
    private static Logger log = Logger.getLogger("Test.log");

    public static void main(String[] args) {

        try {
            FileHandler handler = new FileHandler("/home/bolo/Desktop/test.log");
            handler.setFormatter(new myFormat());
            handler.setEncoding("utf-8");
            log.addHandler(handler);
            log.info("hello word!");
            log.info("你好世界！");
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    static class myFormat extends Formatter {
        public String format(LogRecord record) {
            String log = record.getLoggerName() + "-" + record.getLevel() + "-" + record.getMessage();
            return log;
        }
    }
}
