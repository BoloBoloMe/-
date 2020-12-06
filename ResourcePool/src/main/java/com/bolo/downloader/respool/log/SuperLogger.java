package com.bolo.downloader.respool.log;

import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

public class SuperLogger {
    final private static AtomicReference<Logger> logger = new AtomicReference<>();

    public static AtomicReference<Logger> getLogger() {
        return logger;
    }

    public static void setLogger(Logger log) {
        logger.set(log);
    }
}
