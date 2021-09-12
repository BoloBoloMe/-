package com.bolo.downloader.respool.db.buff;

import java.io.IOException;
import java.io.Writer;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 环形链表写缓冲
 * 线程不安全
 */
public class SimpleWriteBuff implements CycleWriteBuff {
    private static final String LINE_SEPARATOR = System.lineSeparator();
    private static final StringBuffer buffer = new StringBuffer();
    /**
     * 流水号
     */
    private final AtomicInteger serial = new AtomicInteger(1);


    /**
     * 重置流水号到当前最新值
     *
     * @param serial
     */
    public void resetSerialInit(int serial) {
        this.serial.set(serial);
    }

    public void put(String key, String value) {
        // serial
        buffer.append((char) (serial.incrementAndGet() % 65535)).append((char) (serial.get() / 65535))
                // key length
                .append((char) (key.length() % 65535)).append((char) (key.length() / 65535))
                // key & value
                .append(key).append(value).append(LINE_SEPARATOR);
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
        writer.write(buffer.toString());
        buffer.delete(0, buffer.length());
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
}
