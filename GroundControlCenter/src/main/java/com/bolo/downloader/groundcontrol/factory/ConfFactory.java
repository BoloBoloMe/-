package com.bolo.downloader.groundcontrol.factory;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;

public class ConfFactory {
    private static final AtomicReference<String> path = new AtomicReference<>();
    private static final String NOTNULL = "NOTNULL";
    private static final Map<String, String> conf = new TreeMap<>();


    public static void load(String confPath) {
        assert confPath != null : "文件路径不能为空";
        if (!path.compareAndSet(null, confPath)) return;
        conf.put("port", "9999");
        conf.put("url", "http://127.0.0.1:9000/df");
        conf.put("dbFileId", "1");
        conf.put("dbFilePath", "GroundControl/data/");
        conf.put("logPath", "GroundControl/log/");
        conf.put("logFileName", "GroundControlCenter.log");
        conf.put("downloadPath", "~/GroundControl/video/");
        conf.put("staticFilePath", "GroundControl/static/");
        conf.put("mediaPath", "~/");
        conf.put("labelMap", "");
        conf.put("notValidatedPath", "~/GroundControl/video/notValidated/");
        conf.put("downloadRetryTimes", "5");
        conf.put("openSyncTask", Boolean.FALSE.toString());
        int systemProcessors = Runtime.getRuntime().availableProcessors();
        conf.put("mediaBossThreadCount", Integer.toString(systemProcessors));
        conf.put("mediaWorkThreadCount", Integer.toString(systemProcessors * 10));
        final File confFile = new File(path.get());
        try {
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
            }
            final Set<Map.Entry<String, String>> confSet = conf.entrySet();
            for (Map.Entry<String, String> entry : confSet) {
                if (NOTNULL.equals(entry.getValue())) {
                    throw new Error("缺少必要配置项：" + entry.getKey());
                }
            }
            createNotExistsPath(confSet);
        } catch (Exception e) {
            throw new Error("系统配置加载失败");
        }
    }

    public static String get(String key) {
        return conf.get(key);
    }

    private static void createNotExistsPath(Set<Map.Entry<String, String>> confSet) {
        for (Map.Entry<String, String> entry : confSet) {
            String key = entry.getKey(), value = entry.getValue();
            if (key.contains("Path")) {
                File pathFile = new File(value);
                if (!pathFile.exists() || !pathFile.isDirectory()) {
                    if (!pathFile.mkdirs()) {
                        throw new Error("尝试创建文件夹失败！");
                    }
                }
            }
        }
    }

}
