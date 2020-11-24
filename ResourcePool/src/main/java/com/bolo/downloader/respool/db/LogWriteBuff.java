package com.bolo.downloader.respool.db;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * 日志写缓冲
 * 缓冲区遵循FIFO,当缓冲区写满，继续写入数据将会阻塞
 */
public class LogWriteBuff {
    private AtomicReferenceArray<String> buff;
    private AtomicInteger wIndex = new AtomicInteger(-1);
    private AtomicInteger rIndex = new AtomicInteger(-1);
    private int INDEX_MAX;

    /**
     * @param size 缓冲区大小
     */
    public LogWriteBuff(int size) {
        if (size == 0) {
            this.buff = new AtomicReferenceArray<>(2);
            return;
        }
        this.buff = new AtomicReferenceArray<>(size % 2 == 0 ? size : size + 1);
        INDEX_MAX = buff.length() - 1;
    }

    public String pop() {
        String line = null;
        int rInd = rIndex.incrementAndGet();
        if (rInd <= INDEX_MAX) {
            while (null == (line = buff.getAndSet(rInd, null))) ;
        } else {
            // try reset read index
            if (rInd - INDEX_MAX > 1) respite();
            rIndex.compareAndSet(rInd, -1);
        }
        return line;
    }

    public void push(String ele) {
        if (null == ele) return;
        while (true) {
            int wInd = wIndex.incrementAndGet();
            if (wInd <= INDEX_MAX) {
                while (!buff.compareAndSet(wInd, null, ele)) ;
                break;
            } else {
                // try reset write index
                if (wInd - INDEX_MAX > 1) respite();
                wIndex.compareAndSet(wInd, -1);
            }
        }
    }


    private static final Long[] sleeps = new Long[127];

    static {
        Set<Long> sleepSet = new HashSet<>();
        int max = 300;
        int min = 100;
        Random random = new Random();
        while (sleepSet.size() < 127) {
            long s = random.nextInt(max) % (max - min + 1) + min;
            sleepSet.add(s);
        }
        sleepSet.toArray(sleeps);
    }

    /**
     * 随机休眠一小会儿
     */
    private void respite() {
        try {
            Thread.sleep(sleeps[(int) (Thread.currentThread().getId() % 127)]);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
