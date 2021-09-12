package com.bolo.downloader.factory;


import java.io.*;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;

public class ConfFactory {
    private static final AtomicReference<String> path = new AtomicReference<>();
    private static final String NOTNULL = "NOTNULL";

    private static final Map<String, String> conf = new TreeMap<>();


    public static String get(String key) {
        return conf.get(key);
    }

    public static void load(String confPath) {
        assert confPath != null : "文件路径不能为空";
        if (!path.compareAndSet(null, confPath)) return;
        conf.put("port", "9000");
        conf.put("concurrenceTaskNum", "5");
        conf.put("videoPath", "");
        conf.put("dbFilePath", "data/");
        conf.put("writeBuffSize", "8");
        conf.put("staticFilePath", "static/");
        conf.put("youtubeDLPath", "");
        conf.put("dbFileId", "0");
        conf.put("logPath", "log/");
        conf.put("logFileName", "SpaceStation.log");
        try {
            final File confFile = new File(path.get());
            if (confFile.exists()) {
                final Properties properties;
                try (BufferedReader reader = new BufferedReader(new FileReader(confFile))) {
                    properties = new Properties();
                    properties.load(reader);
                } catch (IOException e) {
                    throw new Error("配置文件加载失败！", e);
                }
                for (Map.Entry<Object, Object> entry : properties.entrySet()) {
                    conf.put(entry.getKey().toString(), entry.getValue().toString());
                }
            } else {
                throw new Error("未找到配置文件！" + confFile.getPath());
            }
            for (Map.Entry<String, String> entry : conf.entrySet()) {
                if (NOTNULL.equals(entry.getValue())) {
                    throw new Error("缺少必要配置项：" + entry.getKey());
                }
            }
        } catch (Exception e) {
            throw new Error("系统配置加载失败");
        }
    }

}
