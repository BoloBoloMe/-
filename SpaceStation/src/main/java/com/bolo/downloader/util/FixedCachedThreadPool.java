package com.bolo.downloader.util;

import java.util.concurrent.*;

/**
 * 有最大值的缓存线程池
 */
public class FixedCachedThreadPool extends ThreadPoolExecutor {


    private FixedCachedThreadPool(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
    }

    private FixedCachedThreadPool(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
    }

    private FixedCachedThreadPool(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, handler);
    }

    private FixedCachedThreadPool(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory, RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
    }

    /**
     * 线程池的最大线程数
     *
     * @param maxThreads
     * @return
     */
    public static FixedCachedThreadPool newFixedCachedThreadPool(int maxThreads) {
        FixedCachedThreadPool threadPool = new FixedCachedThreadPool(maxThreads, maxThreads, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
        threadPool.allowCoreThreadTimeOut(true);
        return threadPool;
    }

}
