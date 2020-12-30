package com.bolo.downloader.respool.db.buff;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

public class ReuseCountDownLatch {
    private final Sync sync;

    public ReuseCountDownLatch(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("count < 0");
        } else {
            this.sync = new Sync(count);
        }
    }

    public void await() throws InterruptedException {
        this.sync.acquireSharedInterruptibly(1);
    }

    public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
        return this.sync.tryAcquireSharedNanos(1, unit.toNanos(timeout));
    }

    public void countDown() {
        this.sync.releaseShared(1);
        this.sync.tryInit();
    }

    public long getCount() {
        return (long) this.sync.getCount();
    }

    public String toString() {
        return super.toString() + "[Count = " + this.sync.getCount() + "]";
    }

    private static final class Sync extends AbstractQueuedSynchronizer {
        private static final long serialVersionUID = 4982264981922014374L;
        private final int initCount;

        Sync(int count) {
            initCount = count;
            this.setState(count);
        }

        public boolean tryInit() {
            return compareAndSetState(0, initCount);
        }

        int getCount() {
            return this.getState();
        }

        protected int tryAcquireShared(int acquires) {
            return this.getState() == 0 ? 1 : -1;
        }

        protected boolean tryReleaseShared(int releases) {
            int c;
            int nextc;
            do {
                c = this.getState();
                if (c == 0) {
                    return false;
                }

                nextc = c - 1;
            } while (!this.compareAndSetState(c, nextc));

            return nextc == 0;
        }
    }
}
