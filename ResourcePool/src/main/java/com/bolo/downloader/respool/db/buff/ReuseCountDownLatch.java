package com.bolo.downloader.respool.db.buff;

import java.util.concurrent.CountDownLatch;

public class ReuseCountDownLatch extends CountDownLatch {
    // todo: 实现一个可重复使用的CountDownLatch
    public ReuseCountDownLatch(int count) {
        super(count);
    }
}
