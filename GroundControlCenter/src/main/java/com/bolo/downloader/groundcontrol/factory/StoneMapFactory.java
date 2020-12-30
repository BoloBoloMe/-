package com.bolo.downloader.groundcontrol.factory;

import com.bolo.downloader.respool.db.StoneMap;
import com.bolo.downloader.respool.db.buff.SimpleWriteBuff;

public class StoneMapFactory {
    private static StoneMap stoneMap = null;

    public static StoneMap getObject() {
        return null != stoneMap ? stoneMap : createSingleton();
    }

    private static synchronized StoneMap createSingleton() {
//        new SynchronizedCycleWriteBuff(Integer.valueOf(ConfFactory.get("wrireBuffSize")),
//                Integer.valueOf(ConfFactory.get("putSpedMax")),
//                Integer.valueOf(ConfFactory.get("writeLoopMax")))
        if (null != stoneMap) return stoneMap;
        stoneMap = new StoneMap(ConfFactory.get("dbFilePath"),
                Integer.parseInt(ConfFactory.get("dbFileId")),
                new SimpleWriteBuff());
        stoneMap.loadDbFile();
        return stoneMap;
    }
}
