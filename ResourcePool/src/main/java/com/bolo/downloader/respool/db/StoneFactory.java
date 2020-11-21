package com.bolo.downloader.respool.db;

import java.io.IOException;

public class StoneFactory {
    public static DictStone<String, String> createSimDict(String dbPath, int sens) throws IOException {
        SimpleDictStone stone = new SimpleDictStone(dbPath, sens);
        stone.buff();
        return stone;
    }
}
