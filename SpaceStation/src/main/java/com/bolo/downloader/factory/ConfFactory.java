package com.bolo.downloader.factory;


import java.io.*;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

public class ConfFactory {
    private static final String NOTNULL = "NOTNULL";

    private static final Map<String, String> conf = new TreeMap<>();


    static {
        conf.put("port", "9000");
        conf.put("videoPath", "");
        conf.put("dbFilePath","data/");
        conf.put("wrireBuffSize", "16");
        conf.put("putSpedMax", "1");
        conf.put("writeLoopMax", "1");
        conf.put("youtubeDLPath", NOTNULL);
        conf.put("dbFileId", NOTNULL);

        final File confFile = new File("conf/SpaceStation.conf");
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

        for (Map.Entry<String, String> entry : conf.entrySet()) {
            if (NOTNULL.equals(entry.getValue())) {
                throw new Error("缺少必要配置项：" + entry.getKey());
            }
        }
    }

    public static String get(String key) {
        return conf.get(key);
    }


}
