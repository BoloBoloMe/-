package com.bolo.downloader.respool.db.buff;

import java.util.concurrent.CountDownLatch;

public class ReuseCountDownLatch extends CountDownLatch {
    public ReuseCountDownLatch(int count) {
        super(count);
    }
}
