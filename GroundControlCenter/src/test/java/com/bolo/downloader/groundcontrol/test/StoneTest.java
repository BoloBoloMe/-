package com.bolo.downloader.groundcontrol.test;

import com.bolo.downloader.respool.db.DictStone;
import com.bolo.downloader.respool.db.StoneFactory;

import java.io.IOException;

/**
 * 测试持久化字典API
 */
public class StoneTest {
    public static void main(String[] args) throws IOException {
        DictStone<String,String> dictStone = StoneFactory.createSimDict("D:\\MyResource\\Desktop\\db\\", 5);
        dictStone.put("aaaa", "-1");
        dictStone.put("bbbb", "21");
        dictStone.put("cccc", "3");
        dictStone.put("dddd", "4");
        dictStone.put("eeee", "5");
    }
}
