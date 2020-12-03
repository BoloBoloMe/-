package com.bolo.downloader.respool.log;

import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MyLogger {

    final private AtomicReference<Logger> logger = new AtomicReference<>();

    MyLogger(Logger logger) {
        this.logger.set(logger);
    }

    public void info(String msg) {
        Logger snapshot = logger.get();
        snapshot.info(msg);
    }

    public void info(String msg, String... objs) {
        Logger snapshot = logger.get();
        String convertMsg = String.format(msg, objs);
        snapshot.info(convertMsg);
    }


    public void error(String msg) {
        Logger snapshot = logger.get();
        snapshot.warning(msg);
    }

    public void error(String msg, Throwable throwable) {
        Logger snapshot = logger.get();
        snapshot.log(Level.WARNING, msg, throwable);
    }

    public void error(String msg, String... objs) {
        Logger snapshot = logger.get();
        snapshot.log(Level.WARNING, msg, objs);
    }

    public void setLogger(Logger logger) {
        this.logger.lazySet(logger);
    }
}
