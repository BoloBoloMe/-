package com.bolo.downloader.respool.nio.http;

import com.alibaba.fastjson.JSON;

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

    // 基本类型的 getter start
    public static Long getLong(String name) {
        return getValue(name, Long::parseLong);
    }

    public static Long getLong(String name, Long def) {
        return getValue(name, Long::parseLong, def);
    }

    public static Optional<List<Long>> getLongList(String name) {
        return getValues(name, Long::parseLong);
    }

    public static Integer getInteger(String name) {
        return getValue(name, Integer::parseInt);
    }

    public static Integer getInteger(String name, Integer def) {
        return getValue(name, Integer::parseInt, def);
    }

    public static Optional<List<Integer>> getIntegerList(String name) {
        return getValues(name, Integer::parseInt);
    }

    public static Short getShort(String name) {
        return getValue(name, Short::parseShort);
    }

    public static Short getShort(String name, Short def) {
        return getValue(name, Short::parseShort, def);
    }

    public static Optional<List<Short>> getShortList(String name) {
        return getValues(name, Short::parseShort);
    }

    public static Character getCharacter(String name) {
        return getValue(name, s -> s.isEmpty() ? null : s.charAt(0));
    }

    public static Character getCharacter(String name, Character def) {
        return getValue(name, s -> s.isEmpty() ? null : s.charAt(0), def);
    }

    public static Optional<List<Character>> getCharacterList(String name) {
        return getValues(name, s -> s.isEmpty() ? null : s.charAt(0));
    }

    public static Byte getByte(String name) {
        return getValue(name, Byte::parseByte);
    }

    public static Byte getByte(String name, Byte def) {
        return getValue(name, Byte::parseByte, def);
    }

    public static Optional<List<Byte>> getByteList(String name) {
        return getValues(name, Byte::parseByte);
    }

    public static Boolean getBoolean(String name) {
        return getValue(name, Boolean::parseBoolean);
    }

    public static Boolean getBoolean(String name, Boolean def) {
        return getValue(name, Boolean::parseBoolean, def);
    }

    public static Optional<List<Boolean>> getBooleanList(String name) {
        return getValues(name, Boolean::parseBoolean);
    }

    public static Float getFloat(String name) {
        return getValue(name, Float::parseFloat);
    }

    public static Float getFloat(String name, Float def) {
        return getValue(name, Float::parseFloat, def);
    }

    public static Optional<List<Float>> getFloatList(String name) {
        return getValues(name, Float::parseFloat);
    }

    public static Double getDouble(String name) {
        return getValue(name, Double::parseDouble);
    }

    public static Double getDouble(String name, Double def) {
        return getValue(name, Double::parseDouble, def);
    }

    public static Optional<List<Double>> getDoubleList(String name) {
        return getValues(name, Double::parseDouble);
    }

    // 基本类型的 getter end

    public static String getString(String name) {
        return getValue(name, String::toString);
    }

    public static String getString(String name, String def) {
        return getValue(name, String::toString, def);
    }

    public static Optional<List<String>> getStringList(String name) {
        return getValues(name, String::toString);
    }


    public static <T> T getObjectByJson(String name, Class<T> objClass) {
        return getValue(name, json -> JSON.parseObject(json, objClass));
    }

    public static <T> T getObjectByJson(String name, Class<T> objClass, T def) {
        return getValue(name, json -> JSON.parseObject(json, objClass), def);
    }

    public static <T> Optional<List<T>> getObjectListByJson(String name, Class<T> objClass) {
        return getValues(name, json -> JSON.parseObject(json, objClass));
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
