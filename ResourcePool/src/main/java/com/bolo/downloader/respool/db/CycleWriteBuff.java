package com.bolo.downloader.respool.db;

import java.io.IOException;
import java.io.Writer;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 环形链表写缓冲
 */
public class CycleWriteBuff<K, V> {
    private static final String LINE_SEPARATOR = System.lineSeparator();
    private int size;
    private final int PUT_SPEEDY_MAX;
    private final int WRITE_LOOP_MAX;
    private Note<K, V> entry;
    private final AtomicReference<Note<K, V>> wirteNote = new AtomicReference<>();
    private final AtomicReference<Note<K, V>> readNote = new AtomicReference<>();
    private final AtomicInteger idlingWrite = new AtomicInteger(0);
    private final AtomicInteger allWrite = new AtomicInteger(0);
    private final AtomicInteger idlingPut = new AtomicInteger(0);
    private final AtomicInteger allPut = new AtomicInteger(0);
    /**
     * 流水号
     */
    private final AtomicInteger serial = new AtomicInteger(0);

    private volatile int checkpoint = 0;

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

    public void put(K key, V value) {
        Note<K, V> curr = wirteNote.get();
        for (int i = 1; ; i++) {
            allPut.incrementAndGet();
            if (curr.readable) {
                idlingPut.incrementAndGet();
                curr = curr.next;
                if (i / size > PUT_SPEEDY_MAX) sleep();
            } else {
                synchronized (curr) {
                    if (!curr.readable) {
                        curr.key = key;
                        curr.value = value;
                        curr.serial = serial.incrementAndGet();
                        curr.readable = true;
                        curr.count += 1;
                        wirteNote.compareAndSet(wirteNote.get(), curr);
                        break;
                    } else {
                        idlingPut.incrementAndGet();
                    }
                }
            }
        }
    }


    /**
     * 缓冲区内容写出
     * 行格式：key-length+serial+key+value
     * 长度：
     * key-length —— char*2
     * serial     —— char*2
     * keyvalue   —— char*variable
     *
     * @throws IOException
     */
    public void write(Writer writer) throws IOException {
        Note<K, V> curr = readNote.get();
        for (int i = 0; i / size <= WRITE_LOOP_MAX; i++) {
            allWrite.incrementAndGet();
            boolean idling = true;
            if (curr.readable) {
                synchronized (curr) {
                    if (curr.readable) {
                        idling = false;
                        newRow(curr.key.toString(), curr.value.toString(), curr.serial, writer);
                        // reset this note
                        curr.readable = false;
                        curr.key = null;
                        curr.value = null;
                        curr.serial = null;
                        curr.count += 1;
                        readNote.compareAndSet(readNote.get(), curr);
                    }
                }
            }
            if (idling) idlingWrite.incrementAndGet();
            curr = curr.next;
        }
        writer.flush();
    }

    static void newRow(String key, String value, int serial, Writer writer) throws IOException {
        int keyLen = key.length();
        // serial
        writer.append((char) (serial % 65535)).append((char) (serial / 65535))
                // key length
                .append((char) (keyLen % 65535)).append((char) (keyLen / 65535))
                // key & value
                .append(key).append(value).append(LINE_SEPARATOR);
    }

    /**
     * 写入恢复行
     */
    void recoverRow(String key, String value, Writer writer) throws IOException {
        newRow(key, value, 0, writer);
    }

    int checkpoint() {
        int checkpoint = serial.get();
        serial.set(0);
        return checkpoint;
    }


    /**
     * buffer usage report
     */
    public String usageReport(boolean brief) {
        DecimalFormat df = new DecimalFormat("0.00000");
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
        result.append("{idlingPut=").append(df.format((double) idlingPut.get() / allPut.get())).append(",idlingWrite=").append(df.format((double) idlingWrite.get() / allWrite.get()));
        if (brief) {
            double hitRateSum = 0.000000;
            for (int i = 0; i < size; i++) hitRateSum += tem[i] / optCount;
            result.append(",avgHitRate=").append(df.format(hitRateSum / size));
            result.append('}');
        } else {
            result.append(",hitRate=[");
            for (int i = 0; i < size; i++) {
                result.append(df.format(tem[i] / optCount));
                if (i < size - 1) result.append("]}");
            }
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
        int max = 1000;
        int min = 500;
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
