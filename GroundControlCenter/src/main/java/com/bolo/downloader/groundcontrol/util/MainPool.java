package com.bolo.downloader.groundcontrol.util;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 主循环
 */
public class MainPool {
    /**
     * 主线程池
     */
    private static final ScheduledExecutorService MAIN_THREAD_POOL;

    /**
     * 主时钟
     */
    private static volatile long SYSTEM_TIME_MILLISECOND = System.currentTimeMillis();

    static {
        MAIN_THREAD_POOL =
                Executors.newScheduledThreadPool(1, runnable -> {
                    Thread thread = new Thread(runnable, "main-thread-0");
                    thread.setDaemon(true);
                    return thread;
                });
        MAIN_THREAD_POOL.scheduleWithFixedDelay(() -> SYSTEM_TIME_MILLISECOND = System.currentTimeMillis(), 500, 500, TimeUnit.MILLISECONDS);
    }

    public static ScheduledExecutorService executor() {
        return MAIN_THREAD_POOL;
    }

    public static long currentTimeMillis() {
        return SYSTEM_TIME_MILLISECOND;
    }
}
