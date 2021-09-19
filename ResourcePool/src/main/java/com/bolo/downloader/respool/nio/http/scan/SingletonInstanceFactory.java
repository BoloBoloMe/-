package com.bolo.downloader.respool.nio.http.scan;

public class SingletonInstanceFactory implements TargetInstanceFactory {
    private final Object instance;

    public SingletonInstanceFactory(Object instance) {
        this.instance = instance;
    }

    @Override
    public Object getInstance() {
        return instance;
    }
}
