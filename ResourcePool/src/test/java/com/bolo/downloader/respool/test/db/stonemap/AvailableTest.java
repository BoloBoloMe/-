package com.bolo.downloader.respool.test.db.stonemap;

import com.bolo.downloader.respool.db.StoneMap;
import com.bolo.downloader.respool.db.buff.SimpleWriteBuff;

public class AvailableTest {
    public static void main(String[] args) {
        StoneMap stoneMap = new StoneMap("/home/bolo/program/VideoDownloader/GroundControlCenter/data/", 233, new SimpleWriteBuff());
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
        stoneMap.put("k_4", "x");
        stoneMap.rewriteDbFile();
    }
}
