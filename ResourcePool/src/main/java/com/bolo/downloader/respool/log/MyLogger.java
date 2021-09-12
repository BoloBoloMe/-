package com.bolo.downloader.respool.log;

import java.util.logging.*;

public class MyLogger extends SuperLogger {
    final private Class clazz;

    public MyLogger(Class clazz) {
        this.clazz = clazz;
    }

    public void info(String msg) {
        Logger snapshot = getLogger().get();
        if (snapshot == null) return;
        snapshot.log(Level.INFO, String.format("%s : %s", clazz.getName(), msg), clazz);
    }

    public void info(String msg, Object... objs) {
        Logger snapshot = getLogger().get();
        if (snapshot == null) return;
        String convertMsg = String.format(msg, objs);
        snapshot.log(Level.INFO, String.format("%s : %s", clazz.getName(), convertMsg), clazz);
    }


    public void error(String msg) {
        Logger snapshot = getLogger().get();
        if (snapshot == null) return;
        snapshot.log(Level.WARNING, String.format("%s : %s", clazz.getName(), msg), clazz);
    }

    public void error(String msg, Throwable throwable) {
        Logger snapshot = getLogger().get();
        if (snapshot == null) return;
        snapshot.log(Level.WARNING, String.format("%s : %s", clazz.getName(), msg), throwable);
    }

}
