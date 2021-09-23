package com.bolo.downloader.respool.nio.http.controller.scan.impl;

import java.util.concurrent.ConcurrentHashMap;

public class MethodMapperContainer {
    private final ConcurrentHashMap<String, MethodMapper> mapperTable = new ConcurrentHashMap<>();

    public void put(String path, MethodMapper mapper) {
        mapperTable.put(path, mapper);
    }

    public MethodMapper get(String path) {
        return mapperTable.get(path);
    }

    public void remove(MethodMapper mapper) {
        for (String path : mapper.getPath()) {
            if (mapperTable.get(path) == mapper) {
                mapperTable.remove(path);
            }
        }
    }
}
