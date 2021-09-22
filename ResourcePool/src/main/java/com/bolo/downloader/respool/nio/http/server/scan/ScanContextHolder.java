package com.bolo.downloader.respool.nio.http.server.scan;

import java.util.*;

/**
 * 扫描上下文
 */
public class ScanContextHolder {
    public static final String KEY_ROOT_PATH = "$ROOT_PATH";

    private static final ThreadLocal<Map<String, List<String>>> PARAMETERS_HOLDER = new ThreadLocal<>();

    public static void set(String key, String value) {
        Map<String, List<String>> params = PARAMETERS_HOLDER.get();
        if (Objects.isNull(params)) {
            params = new HashMap<>();
        }
        params.put(key, Collections.singletonList(value));
        PARAMETERS_HOLDER.set(params);
    }

    public static void set(String key, List<String> values) {
        Map<String, List<String>> params = PARAMETERS_HOLDER.get();
        if (Objects.isNull(params)) {
            params = new HashMap<>();
        }
        params.put(key, values);
        PARAMETERS_HOLDER.set(params);
    }

    public static Optional<String> getValue(String key) {
        return getValues(key).map(list -> list.size() > 0 ? list.get(0) : null);
    }


    public static Optional<List<String>> getValues(String key) {
        return Optional.ofNullable(PARAMETERS_HOLDER.get()).map(map -> map.get(key));
    }

    public static void remove(){
        PARAMETERS_HOLDER.remove();
    }
}
