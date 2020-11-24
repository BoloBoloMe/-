package com.bolo.downloader.respool.test.db.stonemap;

import com.bolo.downloader.respool.db.StoneMap;

public class AvailableTest {
    public static void main(String[] args) {
        StoneMap stoneMap = new StoneMap("D:\\MyResource\\Desktop\\data\\", 1, 3);
        stoneMap.put("k_0", "v");
        stoneMap.put("k_1", "v");
        stoneMap.put("k_2", "v");
        stoneMap.put("k_3", "v");
        stoneMap.remove("k_0");
        stoneMap.put("k_3", "nv");
        stoneMap.flushWriteBuff();
        stoneMap.clear();
        stoneMap.loadDbFile();
        stoneMap.put("k_4", "v");
        stoneMap.rewriteDbFile();
    }
}
