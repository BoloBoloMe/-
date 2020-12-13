package com.bolo.downloader.groundcontrol.test;

import java.util.ArrayList;

public class ArrayListTest {
    public static void main(String[] args) {
        ArrayList<String> logWiteBuff = new ArrayList<>();
        logWiteBuff.add("key1");
        logWiteBuff.add("val1");
        logWiteBuff.add("key2");
        logWiteBuff.add("val2");
        logWiteBuff.add("key3");
        logWiteBuff.add("val3");
        System.out.println("原始数组：" + logWiteBuff);
        int tail;
        for (int head = 1; head < logWiteBuff.size(); head += 2) {
            tail = head - 1;
            logWiteBuff.remove(tail);
            logWiteBuff.remove(head);
            System.out.println("第次" + head + "删除元素后的数组" + logWiteBuff);
        }
    }
}
