package com.bolo.downloader.groundcontrol.util;

import com.bolo.downloader.respool.log.LoggerFactory;
import com.bolo.downloader.respool.log.MyLogger;

import java.io.*;

public class FileUtils {
    private static final MyLogger log = LoggerFactory.getLogger(FileUtils.class);

    public static void move(File src, String targetDir) {
        assert src != null && src.isFile() : "文件不存在！";
        assert targetDir != null : "目标目录不存在！";
        File target = new File(targetDir, src.getName());
        log.info("移动文件: [" + src.getAbsolutePath() + "] 至 [" + target.getAbsolutePath() + "]");
        if (target.exists()) target.delete();
        try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(src));
             BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(target))) {
            out.write(in.read());
            out.flush();
            log.info("文件已移动至:" + target.getAbsolutePath());
        } catch (Exception e) {
            log.error("移动文件异常！", e);
        }
    }
}
