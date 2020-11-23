package com.bolo.downloader.groundcontrol.test;


import com.bolo.downloader.respool.db.StoneMap;

/**
 * 测试持久化字典API
 */
public class StoneMapTest {
    public static void main(String[] args) {
        StoneMap map = new StoneMap("D:\\MyResource\\Desktop\\data\\", 1, 16);
        map.load();
        for (int i = 0; i < 100; i++) map.put("key_" + i, "v_" + i);
        for (int i = 0; i < 100; i++) if (i % 2 == 0) map.remove("key_" + i);
        map.flushBuff();

        map.rewriteDbFile();
        map.put("needle_1", "探测探测1");
        map.put("needle_2", "探测探测2");
        map.flushBuff();
    }
}
