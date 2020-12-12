package com.bolo.downloader.groundcontrol.factory;


import com.bolo.downloader.groundcontrol.Cantor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

public class ConfFactory {
    private static final String NOTNULL = "NOTNULL";
    private static final Map<String, String> conf = new TreeMap<>();


    static {
        conf.put("url", "http://127.0.0.1:9000/df");
        conf.put("dbFileId", "100");
        conf.put("dbFilePath", "/data/");
        conf.put("logPath", "/log/");
        conf.put("logFileName", "GroundControlCenter.log");
        conf.put("filePath", "");
        conf.put("wrireBuffSize", "10");
        conf.put("putSpedMax", "1");
        conf.put("writeLoopMax", "1");
        conf.put("rsa", "0");

        final File confFile = new File(Cantor.CONF_FILE_PATH);
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
    }

    public static String get(String key) {
        return conf.get(key);
    }


}
