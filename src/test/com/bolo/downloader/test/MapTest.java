package com.bolo.downloader.test;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class MapTest {
    @Test
    public void testPut() {
        Map<String, String> map = new HashMap<>();
        map.put("key", "1");
        map.replace("key2", "2");
        System.out.println(map);
    }
}
