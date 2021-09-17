package com.bolo.downloader.respool.nio.http.scan;

import io.netty.handler.codec.http.HttpMethod;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;

public class MethodMapper {
    final private String path;
    final private HttpMethod[] allowedMethods;
    final private Method targetMethod;
    final private Object targetInstance;
    private final Class<?> targetClass;


    public MethodMapper(String path, HttpMethod[] allowedMethods, Method targetMethod, Object targetInstance, Class<?> targetClass) {
        this.path = path;
        this.allowedMethods = allowedMethods;
        this.targetMethod = targetMethod;
        this.targetInstance = targetInstance;
        this.targetClass = targetClass;
    }

    public String getPath() {
        return path;
    }

    public Optional<HttpMethod> getIfExist(HttpMethod httpMethod) {
        int index = Arrays.binarySearch(allowedMethods, httpMethod, HttpMethod::compareTo);
        return index > -1 && allowedMethods.length > index ? Optional.of(allowedMethods[index]) : Optional.empty();
    }

    public String getAllowedMethods() {
        return Arrays.toString(allowedMethods);
    }

    public Method getTargetMethod() {
        return targetMethod;
    }

    public Object getTargetInstance() {
        return targetInstance;
    }

    public Class<?> getTargetClass() {
        return targetClass;
    }
}
