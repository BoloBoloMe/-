package com.bolo.downloader.respool.test.db;

import java.util.concurrent.CountDownLatch;

public class CountDownLatchTest {
    public static void main(String[] args) {
        CountDownLatch latch = new CountDownLatch(1);
        System.out.println(latch.getCount());
        latch.countDown();
        System.out.println(latch.getCount());
        latch.countDown();
        System.out.println(latch.getCount());
    }
}
