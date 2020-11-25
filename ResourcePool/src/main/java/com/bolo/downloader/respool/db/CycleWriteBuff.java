package com.bolo.downloader.respool.db;

import java.io.IOException;
import java.io.Writer;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 环形日志写链表
 */
public class CycleWriteBuff<K, V> {
    private static final String LINE_SEPARATOR = System.lineSeparator();
    private int size;
    private final int PUT_SPEEDY_MAX;
    private final int WRITE_LOOP_MAX;
    private Note<K, V> entry;
    private final AtomicReference<Note<K, V>> wirteNote = new AtomicReference<>();
    private final AtomicReference<Note<K, V>> readNote = new AtomicReference<>();


    public CycleWriteBuff(int size, int putSpeedyMax, int writeLoopMax) {
        this.size = size;
        this.PUT_SPEEDY_MAX = putSpeedyMax;
        this.WRITE_LOOP_MAX = writeLoopMax;
        Note<K, V> curr = entry = new Note<>();
        while (size-- >= 1) {
            curr.next = new Note<>();
            curr = curr.next;
            if (size == 0) {
                curr.next = entry;
            }
        }
        wirteNote.set(entry);
        readNote.set(entry);
    }

    public void put(K key, V value, Integer serial) {
        Note<K, V> curr = wirteNote.get();
        for (int i = 1; ; i++) {
            if (curr.readable) {
                curr = curr.next;
                if (i / size > PUT_SPEEDY_MAX) sleep();
            } else {
                synchronized (curr) {
                    if (!curr.readable) {
                        curr.key = key;
                        curr.value = value;
                        curr.serial = serial;
                        curr.readable = true;
                        curr.count += 1;
                        wirteNote.compareAndSet(wirteNote.get(), curr);
                        break;
                    }
                }
            }
        }
    }

    public void write(Writer writer) throws IOException {
        Note<K, V> curr = readNote.get();
        for (int i = 0; i / size <= WRITE_LOOP_MAX; i++) {
            if (curr.readable) {
                synchronized (curr) {
                    if (curr.readable) {
//                        writer.append(curr.key.toString()).append(curr.value.toString()).append(LINE_SEPARATOR);
                        writer.flush();
                        curr.readable = false;
                        curr.key = null;
                        curr.value = null;
                        curr.serial = null;
                        curr.count += 1;
                        readNote.compareAndSet(readNote.get(), curr);
                    }
                }
            }
            curr = curr.next;
        }
//        writer.flush();
    }

    /**
     * 返回节点命中率
     */
    public String nodeHitRate() {
        double[] tem = new double[size];
        int optCount = 0;
        Note<K, V> curr = entry;
        for (int i = 0; i < size; i++) {
            int currNodeCount = curr.count;
            tem[i] = currNodeCount;
            optCount += currNodeCount;
            curr = curr.next;
        }
        StringBuilder result = new StringBuilder();
        DecimalFormat df = new DecimalFormat("0.00");
        for (int i = 0; i < size; i++) {
            result.append(df.format(tem[i] * 100 / optCount)).append('%');
            if (i < size - 1) result.append(", ");
        }
        return result.toString();
    }

    private static class Note<K, V> {
        private Note<K, V> next;
        private boolean readable = false;
        private K key;
        private V value;
        private Integer serial;
        private int count = 0;
    }


    private static final Long[] sleeps = new Long[127];

    static {
        Set<Long> sleepSet = new HashSet<>();
        int max = 600;
        int min = 200;
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
    private void sleep() {
        try {
            Thread.sleep(sleeps[(int) (Thread.currentThread().getId() % 127)]);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
