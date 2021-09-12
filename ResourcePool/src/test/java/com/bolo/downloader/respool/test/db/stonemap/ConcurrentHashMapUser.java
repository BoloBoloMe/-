package com.bolo.downloader.respool.test.db.stonemap;

import java.util.concurrent.ConcurrentHashMap;

public class ConcurrentHashMapUser {
    public static void main(String[] args) {
        ConcurrentHashMap<String,String> map = new ConcurrentHashMap<>();
        map.put("k1","v1");
        map.put("k1","v2");
        map.put("k1","v3");
    }
}
