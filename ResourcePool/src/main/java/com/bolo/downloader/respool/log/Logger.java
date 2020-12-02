package com.bolo.downloader.respool.log;

public class Logger {
    public void info(String line, Object... params) {
        System.out.println(line);
    }

    public void error(String line, Object... params) {
        System.err.println(line);
    }
}
