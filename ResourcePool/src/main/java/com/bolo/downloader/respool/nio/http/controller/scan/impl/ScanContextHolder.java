package com.bolo.downloader.respool.nio.http.controller.scan.impl;

import java.util.*;

/**
 * 扫描上下文
 */
public class ScanContextHolder {
    public static final String KEY_ROOT_PATH = "$ROOT_PATH";
    public static final String KEY_METHOD_INVOKER = "$METHOD_INVOKER";

    private static final ThreadLocal<Map<String, List<?>>> PARAMETERS_HOLDER = new ThreadLocal<>();

    public static <T> void set(String key, T value) {
        Map<String, List<?>> params = PARAMETERS_HOLDER.get();
        if (Objects.isNull(params)) {
            params = new HashMap<>();
        }
        params.put(key, Collections.singletonList(value));
        PARAMETERS_HOLDER.set(params);
    }

    public static <T> void set(String key, List<T> values) {
        Map<String, List<?>> params = PARAMETERS_HOLDER.get();
        if (Objects.isNull(params)) {
            params = new HashMap<>();
        }
        params.put(key, values);
        PARAMETERS_HOLDER.set(params);
    }

    public static <T> Optional<T> getValue(String key, Class<T> vClass) {
        return getValues(key, vClass).map(list -> list.size() > 0 ? list.get(0) : null);
    }


    public static <T> Optional<List<T>> getValues(String key, Class<T> vClass) {
        return Optional.ofNullable(PARAMETERS_HOLDER.get()).map(map -> (List<T>) map.get(key));
    }

    public static void remove() {
        PARAMETERS_HOLDER.remove();
    }
}
