package com.bolo.downloader.respool.test.db.writebuff;


import com.bolo.downloader.respool.db.LogWriteBuff;

import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * 测试持久化字典API
 */
public class CapabilityTest {
    private static AtomicReferenceArray<String> array = new AtomicReferenceArray<>(1);
    private static LogWriteBuff buff = new LogWriteBuff(3);

    public static void main(String[] args) {
        buff.push("1");
        buff.push("2");
        buff.push("3");
        System.out.println(buff.pop());
        System.out.println(buff.pop());
        System.out.println(buff.pop());
        System.out.println(buff);
    }
}