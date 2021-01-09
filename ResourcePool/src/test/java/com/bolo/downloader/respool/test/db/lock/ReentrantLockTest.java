package com.bolo.downloader.respool.test.db.lock;

import java.util.concurrent.locks.ReentrantLock;

public class ReentrantLockTest {
    public static void main(String[] args) {
        ReentrantLock lock = new ReentrantLock();
        try {
            lock.lock();
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
