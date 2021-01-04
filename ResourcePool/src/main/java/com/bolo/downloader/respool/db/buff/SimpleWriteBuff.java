package com.bolo.downloader.respool.db.buff;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * 环形链表写缓冲
 * 线程不安全
 */
public class SimpleWriteBuff implements CycleWriteBuff {
    private static final String LINE_SEPARATOR = System.lineSeparator();
    private static final ArrayList<Entry> buffPool = new ArrayList<>();
    /**
     * 流水号
     */
    private volatile int serial = 0;


    /**
     * 重置流水号到当前最新值
     *
     * @param serial
     */
    public void resetSerialInit(int serial) {
        this.serial = serial;
    }

    public void put(String key, String value) {
        Entry entry = new Entry();
        entry.serial = ++serial;
        entry.key = key;
        entry.value = value;
        buffPool.add(entry);
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
        Iterator<Entry> iterator = buffPool.iterator();
        while (iterator.hasNext()) {
            Entry entry = iterator.next();
            newRow(entry.key, entry.value, entry.serial, writer);
            iterator.remove();
        }
        writer.flush();
    }

    private static void newRow(String key, String value, int serial, Writer writer) throws IOException {
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
    public void recoverRow(String key, String value, Writer writer) throws IOException {
        newRow(key, value, 0, writer);
    }

    public int checkpoint() {
        int checkpoint = serial;
        serial = 0;
        return checkpoint;
    }


    /**
     * buffer usage report
     */
    public String usageReport(boolean brief) {
        return "";
    }

    private static class Entry {
        private String key;
        private String value;
        private Integer serial;
    }
}
