package com.bolo.downloader.respool.concurrent;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 可变线程池
 *
 * @author: luojingyan
 * create time: 2021/10/25 7:40 下午
 **/
public class VariableThreadPoolExecutor implements ExecutorService {
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
    private final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    private volatile ExecutorService executorService;

    public VariableThreadPoolExecutor(ExecutorService executorService) {
        this.executorService = executorService;
        this.isRunning.set(true);
    }


    @Override
    public void shutdown() {
        isRunning.set(false);
        getExecutor().shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        isRunning.set(false);
        return getExecutor().shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return getExecutor().isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return getExecutor().isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return getExecutor().awaitTermination(timeout, unit);
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return getExecutor().submit(task);
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return getExecutor().submit(task, result);
    }

    @Override
    public Future<?> submit(Runnable task) {
        return getExecutor().submit(task);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return getExecutor().invokeAll(tasks);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        return getExecutor().invokeAll(tasks, timeout, unit);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return getExecutor().invokeAny(tasks);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return getExecutor().invokeAny(tasks, timeout, unit);
    }

    @Override
    public void execute(Runnable command) {
        getExecutor().execute(command);
    }

    public void setExecutor(ExecutorService executorService) {
        assert null != executorService;
        final ExecutorService oldExecutor = this.executorService;
        try {
            writeLock.lock();
            if (executorService == oldExecutor) {
                return;
            }
            if (!isRunning.get()) {
                throw new IllegalStateException("executor not is running");
            }
            oldExecutor.shutdown();
            this.executorService = executorService;
        } finally {
            writeLock.unlock();
        }
    }

    public ExecutorService getExecutor() {
        try {
            readLock.lock();
            if (null == executorService) {
                throw new NullPointerException("executor service is null");
            }
            return executorService;
        } finally {
            readLock.unlock();
        }
    }

}
