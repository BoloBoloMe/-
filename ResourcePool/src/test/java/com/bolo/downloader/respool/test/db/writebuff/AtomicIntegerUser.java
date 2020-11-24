package com.bolo.downloader.respool.test.db.writebuff;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class AtomicIntegerUser {
    private static AtomicInteger integer = new AtomicInteger(0);
    private static ExecutorService workers = Executors.newCachedThreadPool();
    private static CountDownLatch countDownLatch_Go = new CountDownLatch(1);
    private static CountDownLatch countDownLatch_Stop = new CountDownLatch(300);
    private static ArrayList<Integer> results = new ArrayList<>(300);

    public static void main(String[] args) throws InterruptedException {
        for (int threadNum = 0; threadNum < 300; threadNum++) {
            workers.execute(() -> {
                try {
                    countDownLatch_Go.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                results.add(integer.incrementAndGet());
                countDownLatch_Stop.countDown();
            });
        }
        countDownLatch_Go.countDown();
        countDownLatch_Stop.await();
        System.out.println("main thread : integer equals " + integer.get());
        for (int i = 0; i < results.size(); i++) {
            for (int j = i + 1; j < results.size(); j++) {
                Integer repeat;
                if (results.get(i).equals(repeat = results.get(j))) {
                    System.out.println("出现重复的数值：" + repeat);
                }
            }
        }
        System.out.println("测试结束");
    }
}
