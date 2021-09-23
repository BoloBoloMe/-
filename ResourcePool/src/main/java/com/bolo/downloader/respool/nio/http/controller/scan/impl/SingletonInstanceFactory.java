package com.bolo.downloader.respool.nio.http.controller.scan.impl;

import com.bolo.downloader.respool.log.LoggerFactory;
import com.bolo.downloader.respool.log.MyLogger;
import com.bolo.downloader.respool.nio.http.controller.scan.TargetInstanceFactory;

import java.util.Objects;
import java.util.Optional;

public class SingletonInstanceFactory implements TargetInstanceFactory {
    private static final MyLogger log = LoggerFactory.getLogger(SingletonInstanceFactory.class);
    private final Class<?> targetClass;

    private volatile Object instance;

    public SingletonInstanceFactory(Class<?> targetClass) {
        this.targetClass = targetClass;
    }

    @Override
    public Optional<Object> getInstance() {
        if (Objects.nonNull(instance)) {
            return Optional.of(instance);
        }
        synchronized (this) {
            if (Objects.isNull(instance)) {
                try {
                    instance = targetClass.newInstance();
                } catch (InstantiationException | IllegalAccessException e) {
                    log.error("build target instance is error:", e);
                }
            }
        }
        return Optional.ofNullable(instance);
    }
}
