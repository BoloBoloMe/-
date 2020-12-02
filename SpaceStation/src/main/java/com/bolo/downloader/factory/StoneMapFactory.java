package com.bolo.downloader.factory;

import com.bolo.downloader.respool.db.StoneMap;

public class StoneMapFactory {
    private static StoneMap stoneMap = null;

    public static StoneMap getObject() {
        return null != stoneMap ? stoneMap : createSingleton();
    }

    private static synchronized StoneMap createSingleton() {
        if (null != stoneMap) return stoneMap;
        return stoneMap = new StoneMap(ConfFactory.get("dbFilePath"),
                Integer.valueOf(ConfFactory.get("dbFileId")),
                Integer.valueOf(ConfFactory.get("wrireBuffSize")),
                Integer.valueOf(ConfFactory.get("putSpedMax")),
                Integer.valueOf(ConfFactory.get("writeLoopMax")));
    }
}
