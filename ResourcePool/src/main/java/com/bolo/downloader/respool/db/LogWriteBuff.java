package com.bolo.downloader.respool.db;

import java.util.Arrays;

/**
 * 日志写缓冲
 * 缓冲区遵循FIFO,当缓冲区写满，继续写入数据将会丢弃当前缓冲的所有数据
 */
public class LogWriteBuff {
    private int count = 0;
    private String[] buff;
    private int wIndex = 0;
    private int rindex = 0;
    private int INDEX_MAX;
    private int CHECKPOINT = 0;

    /**
     * @param size 缓冲区大小
     */
    LogWriteBuff(int size) {
        if (size == 0) {
            this.buff = new String[2];
            return;
        }
        this.buff = new String[size % 2 == 0 ? size : size + 1];
        INDEX_MAX = buff.length - 1;
    }

    /**
     * 缓冲区已写满时返回true
     */
    boolean isFulfil() {
        return wIndex > INDEX_MAX;
    }

    void push(String key, String value) {
        setValue(getLine(key, value));
    }

    static String getLine(String key, String value) {
        StringBuilder ele = new StringBuilder(10 + key.length() + (null == value ? 0 : value.length()));
        // length
        ele.append(key.length());
        while (ele.length() < 10) ele.insert(0, "0");
        ele.append(key).append(value);
        return ele.toString();
    }

    synchronized private void setValue(String ele) {
        if (wIndex > INDEX_MAX) {
            wIndex = 0;
            rindex = 0;
        }
        buff[wIndex++] = ele;
        count++;
    }

    /**
     * 返回缓冲区中还未读取的元素个数
     *
     * @return
     */
    int length() {
        return wIndex - rindex;
    }

    synchronized String pop() {
        if (rindex < wIndex) {
            return buff[rindex++];
        } else {
            return null;
        }
    }


    synchronized void checkpoint() {
        CHECKPOINT = count;
        count = 0;
    }

    int getCheckpoint() {
        return CHECKPOINT;
    }

    synchronized void increaseCount() {
        ++count;
    }

    int getCount() {
        return count;
    }

    @Override
    public String toString() {
        return "LogWriteBuff{" +
                "buff=" + Arrays.toString(buff) +
                '}';
    }
}
