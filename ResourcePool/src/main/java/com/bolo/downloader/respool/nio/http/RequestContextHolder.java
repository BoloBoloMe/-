package com.bolo.downloader.respool.nio.http;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 用来获取当前请求发送的参数的工具类
 *
 * @author: luojingyan
 * create time: 2021/9/18 12:51 下午
 **/
public class RequestContextHolder {
    private static final ThreadLocal<Map<String, List<String>>> PARAMETERS_HOLDER = new ThreadLocal<>();

    public static void setParameters(Map<String, List<String>> parameter) {
        PARAMETERS_HOLDER.set(parameter);
    }


    public static void remove() {
        if (Objects.nonNull(PARAMETERS_HOLDER.get())) {
            PARAMETERS_HOLDER.remove();
        }
    }

    public static Map<String, List<String>> getParameters() {
        return PARAMETERS_HOLDER.get();
    }

    // todo 各种基本类型的 getter
    public static Integer getInteger(String name) {
        return getValue(name, Integer::parseInt);
    }

    public static Integer getInteger(String name, Integer def) {
        return getValue(name, Integer::parseInt, def);
    }

    public static Optional<List<Integer>> getIntegerList(String name) {
        return getValues(name, Integer::parseInt);
    }


    /* 获取参数的公共方法 start */
    private static <T> T getValue(String name, Function<String, T> parse, T def) {
        List<String> valueList = getParameters().get(name);
        if (Objects.isNull(valueList) || valueList.isEmpty()) {
            return def;
        }
        String value = valueList.get(0);
        return Objects.isNull(value) ? def : parse.apply(value);
    }

    private static <T> T getValue(String name, Function<String, T> parse) {
        return getValue(name, parse, null);
    }

    private static <T> Optional<List<T>> getValues(String name, Function<String, T> parse) {
        return Optional.ofNullable(getParameters().get(name))
                .map(list -> list.stream().map(parse).collect(Collectors.toList()));
    }
    /* 获取参数的公共方法 end */
}
