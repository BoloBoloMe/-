package com.bolo.downloader.respool.log;



public class MockLogger extends MyLogger {

    public MockLogger(Class clazz) {
        super(clazz);
    }

    public void info(String msg) {
    }

    public void info(String msg, Object... objs) {
    }


    public void error(String msg) {
    }

    public void error(String msg, Throwable throwable) {
    }

}
