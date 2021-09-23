package com.bolo.downloader.respool.nio.http.controller.scan.impl;

import com.bolo.downloader.respool.log.LoggerFactory;
import com.bolo.downloader.respool.log.MyLogger;
import com.bolo.downloader.respool.nio.http.controller.scan.TargetInstanceFactory;

import java.util.Optional;

public class PrototypeInstanceFactory implements TargetInstanceFactory {
    private static final MyLogger log = LoggerFactory.getLogger(PrototypeInstanceFactory.class);


    final private Class<?> targetClass;

    public PrototypeInstanceFactory(Class<?> targetClass) {
        this.targetClass = targetClass;
    }


    @Override
    public Optional<Object> getInstance() {
        try {
            return Optional.of(targetClass.newInstance());
        } catch (InstantiationException | IllegalAccessException e) {
            log.error("build target instance is error:", e);
        }
        return Optional.empty();
    }
}
