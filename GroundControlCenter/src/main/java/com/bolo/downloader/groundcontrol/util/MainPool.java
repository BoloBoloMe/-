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
        int coreThreads = Math.max(2, Runtime.getRuntime().availableProcessors());
        MAIN_THREAD_POOL =
                Executors.newScheduledThreadPool(coreThreads, runnable -> {
                    Thread thread = new Thread(runnable, "main-thread-0");
                    thread.setDaemon(false);
                    return thread;
                });
        MAIN_THREAD_POOL.scheduleWithFixedDelay(() -> SYSTEM_TIME_MILLISECOND = System.currentTimeMillis(), 0, 500, TimeUnit.MILLISECONDS);
    }

    public static ScheduledExecutorService executor() {
        return MAIN_THREAD_POOL;
    }

    public static long currentTimeMillis() {
        return SYSTEM_TIME_MILLISECOND;
    }
}
