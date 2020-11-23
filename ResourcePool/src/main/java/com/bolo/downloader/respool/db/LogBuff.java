package com.bolo.downloader.respool.db;

import java.util.Arrays;

public class LogBuff {
    private int count = 0;
    private String[] buff;
    private int wIndex = 0;
    private int rindex = 0;
    private int INDEX_MAX;
    private int CHECKPOINT = 0;

    LogBuff(int size) {
        if (size == 0) {
            this.buff = new String[2];
            return;
        }
        this.buff = new String[size % 2 == 0 ? size : size + 1];
        INDEX_MAX = buff.length - 1;
    }

    boolean writeFulfil() {
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
        return "LogBuff{" +
                "buff=" + Arrays.toString(buff) +
                '}';
    }
}
