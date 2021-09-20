package com.bolo.downloader.respool.nio.http.server.scan;

import io.netty.handler.codec.http.HttpMethod;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

public class MethodMapper {
    final private List<String> path;
    final private List<HttpMethod> allowedMethods;
    final private Method targetMethod;
    final private TargetInstanceFactory targetInstanceFactory;
    private final Class<?> targetClass;


    public MethodMapper(List<String> path, List<HttpMethod> allowedMethods, Method targetMethod, TargetInstanceFactory targetInstanceFactory, Class<?> targetClass) {
        this.path = path;
        this.allowedMethods = allowedMethods;
        this.targetMethod = targetMethod;
        this.targetInstanceFactory = targetInstanceFactory;
        this.targetClass = targetClass;
    }

    public String[] getPath() {
        return path.toArray(new String[0]);
    }

    public Optional<HttpMethod> getIfExist(HttpMethod httpMethod) {
        return allowedMethods.contains(httpMethod) ? Optional.of(httpMethod) : Optional.empty();
    }

    public String getAllowedMethods() {
        return allowedMethods.toString();
    }

    public Method getTargetMethod() {
        return targetMethod;
    }

    public Optional<Object> getTargetInstance() {
        return targetInstanceFactory.getInstance();
    }

    public Class<?> getTargetClass() {
        return targetClass;
    }
}
