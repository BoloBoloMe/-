package com.bolo.downloader.factory;

import com.bolo.downloader.respool.db.StoneMap;
import com.bolo.downloader.respool.db.buff.SynchronizedCycleWriteBuff;

public class StoneMapFactory {
    private static StoneMap stoneMap = null;

    public static StoneMap getObject() {
        return null != stoneMap ? stoneMap : createSingleton();
    }

    private static synchronized StoneMap createSingleton() {
        if (null != stoneMap) return stoneMap;
        stoneMap = new StoneMap(ConfFactory.get("dbFilePath"),
                Integer.parseInt(ConfFactory.get("dbFileId")),
                new SynchronizedCycleWriteBuff(Integer.parseInt(ConfFactory.get("writeBuffSize"))));
        stoneMap.loadDbFile();
        return stoneMap;
    }
}
