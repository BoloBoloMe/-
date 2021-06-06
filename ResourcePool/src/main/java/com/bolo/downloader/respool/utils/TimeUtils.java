package com.bolo.downloader.respool.utils;

import java.util.Date;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class TimeUtils {
    private static final AtomicLong time = new AtomicLong(System.currentTimeMillis());

    public static long getTime() {
        return time.get();
    }

    public static Date getDate() {
        return new Date(getTime());
    }

    public static void start() {
        start(new ScheduledThreadPoolExecutor(1));
    }

    public static void start(ScheduledThreadPoolExecutor scheduledExecutor) {
        scheduledExecutor.scheduleAtFixedRate(() -> time.set(System.currentTimeMillis()), 1000L, 1000L, TimeUnit.MILLISECONDS);
    }
}
