package com.bolo.downloader.respool.test.db.threadpool;


import com.bolo.downloader.respool.concurrent.VariableThreadPoolExecutor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 可变的线程池测试
 *
 * @author: luojingyan
 * create time: 2021/10/25 7:36 下午
 **/
public class VariableThreadPoolTest {
    public static void main(String[] args) {
        int nThread = 1;
        VariableThreadPoolExecutor variableThreadPoolExecutor = new VariableThreadPoolExecutor(newExecutorService(nThread));

        Map<Long, AtomicInteger> counter = new ConcurrentHashMap<>(10);
        ExecutorService submitter = Executors.newFixedThreadPool(10);
        for (int i = 0; i < 10; i++) {
            submitter.execute(() -> {
                for (int c = 0; c < 8; c++) {
                    sleep(500);
                    variableThreadPoolExecutor.execute(() -> {
                        long key = Thread.currentThread().getId();
                        AtomicInteger count = counter.getOrDefault(key, new AtomicInteger(0));
                        count.incrementAndGet();
                        counter.put(key, count);
                    });
                }
            });
        }

        for (int timer = 1; timer < 20; timer++) {
            if (timer % 5 == 0) {
                variableThreadPoolExecutor.setExecutor(newExecutorService(nThread *= 10));
                System.out.println("change thread pool.");
            }
            sleep(1000);
            System.out.println(counter.size());
        }
    }


    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static final AtomicInteger THREAD_NUM = new AtomicInteger(0);

    private static ExecutorService newExecutorService(int nThread) {
        return Executors.newFixedThreadPool(nThread, r -> {
            Thread thread = new Thread(r);
            thread.setName("VariableThreads_" + THREAD_NUM.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        });
    }
}
