package com.bolo.downloader.respool.db.buff;

import java.io.IOException;
import java.io.Writer;
import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.*;

/**
 * 环形链表写缓冲
 * 相比SynchronizedCycleWriteBuff要有更高的并发性能
 */
public class ConcurrentCycleWriteBuff implements CycleWriteBuff {
    private static final String LINE_SEPARATOR = System.lineSeparator();
    private int size;
    private final int MAX_FAST_PUT;
    private final int MAX_WRITE_LOOP;
    private Note entry;
    private final AtomicReference<Note> writeNode = new AtomicReference<>();
    private final AtomicReference<Note> readNode = new AtomicReference<>();
    private final AtomicInteger idlingOpt = new AtomicInteger(0);
    private final AtomicInteger totalOpt = new AtomicInteger(0);
    private final ReuseCountDownLatch latch = new ReuseCountDownLatch(1);
    /**
     * 流水号
     */
    private final AtomicInteger serial = new AtomicInteger(0);

    private volatile int checkpoint = 0;


    public ConcurrentCycleWriteBuff(int size, int putSpeedyMax, int writeLoopMax) {
        this.size = size;
        this.MAX_FAST_PUT = putSpeedyMax;
        this.MAX_WRITE_LOOP = writeLoopMax;
        Note curr = entry = new Note();
        while (size-- >= 1) {
            curr.next = new Note();
            curr = curr.next;
            if (size == 0) {
                curr.next = entry;
            }
        }
        writeNode.set(entry);
        readNode.set(entry);
    }

    /**
     * 重置流水号到当前最新值
     *
     * @param serial
     */
    public void resetSerialInit(int serial) {
        this.serial.set(serial);
    }


    public void put(String key, String value) {
        Note curr = writeNode.get();
        for (int i = 1; ; i++) {
            totalOpt.incrementAndGet();
            if (curr.setContent(key, value, serial.incrementAndGet())) {
                writeNode.compareAndSet(writeNode.get(), curr);
            } else {
                idlingOpt.incrementAndGet();
                curr = curr.next;
                if (i / size > MAX_FAST_PUT) awaitLatch();
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
        Note curr = readNode.get();
        for (int i = 0; i / size <= MAX_WRITE_LOOP; i++) {
            totalOpt.incrementAndGet();
            if (curr.purgeContent(writer)) {
                readNode.compareAndSet(readNode.get(), curr);
            } else {
                idlingOpt.incrementAndGet();
            }
            curr = curr.next;
        }
        latch.countDown();
        writer.flush();
    }

    /**
     * 写入恢复行
     */
    public void recoverRow(String key, String value, Writer writer) throws IOException {
        int keyLen = key.length();
        // serial
        writer.append((char) 0).append((char) 0)
                // key length
                .append((char) (keyLen % 65535)).append((char) (keyLen / 65535))
                // key & value
                .append(key).append(value).append(LINE_SEPARATOR);
    }


    public int checkpoint() {
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
        Note curr = entry;
        for (int i = 0; i < size; i++) {
            int currNodeCount = curr.count;
            tem[i] = currNodeCount;
            optCount += currNodeCount;
            curr = curr.next;
        }
        StringBuilder result = new StringBuilder();
        result.append("idlingRate =").append(df.format((double) idlingOpt.get() / totalOpt.get()));
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

    private static class Note {
        private Note next;
        /**
         * readableFlag+serial+keyLength+key+value
         */
        private AtomicReference<AtomicLongArray> content = new AtomicReference<>(new AtomicLongArray(512));
        private int count = 0;

        /**
         * 写操作
         */
        boolean setContent(String key, String value, int serial) {
            final long currentThreadId = Thread.currentThread().getId();
            AtomicLongArray array = content.get();
            if (!array.compareAndSet(0, 0L, currentThreadId)) {
                return false;
            }
            // 当前线程已经独占该数组
            int contentLength = key.length() + value.length() + 5;
            if (array.length() < contentLength) {
                // 尝试扩容
                AtomicLongArray oldArray = array;
                array = new AtomicLongArray(contentLength);
                array.set(0, 0);
                setArray(array, key, value, serial);
                if (!content.compareAndSet(oldArray, array)) {
                    return false;
                }
            }
            // 现有数组能装下当前的内容,设置内容
            setArray(array, key, value, serial);
            return true;
        }

        /**
         * 读操作
         * 该方法的执行必须是串行的
         */
        synchronized boolean purgeContent(Writer writer) throws IOException {
            AtomicLongArray array = content.get();
            long flag = array.get(0);
            if (flag == 0) {
                return false;
            }
            // 当前节点可读
            for (int i = 1; i < array.length(); i++)
                writer.write((int) array.get(i));
            array.set(0, 0);
            return true;
        }

    }

    private static void setArray(AtomicLongArray array, String key, String value, int serial) {
        array.set(1, serial % 65535);
        array.set(2, serial / 65535);
        array.set(3, key.length() % 65535);
        array.set(4, key.length() / 65535);
        for (int k = 0; k < key.length(); k++) array.set(5 + k, key.charAt(k));
        int firstValueIndex = 4 + key.length();
        for (int v = 0; v < value.length(); v++) array.set(firstValueIndex + v, key.charAt(v));
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
    private void awaitLatch() {
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
