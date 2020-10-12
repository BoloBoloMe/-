package com.bolo.downloader;


import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

@Slf4j
public class Terminal {
    private static final String IDIOMATIC = "java@localhost:$  {}";

    private static final String flag = "[download] 100% ";

    public static boolean execYoutubeDL(String url, String filePath) {
        Process process = null;
        BufferedReader commandLineReader = null;
        try {
            String command = String.format("youtube-dl --exec \"mv {} %s{}\"  %s", filePath, url);
            log.info(IDIOMATIC, command);
            process = Runtime.getRuntime().exec(command);
            commandLineReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            if (commandLineReader.ready()) {
                String export;
                while ((export = commandLineReader.readLine()) != null) {
                    log.info(IDIOMATIC, export);
                    if (export.contains(flag)) {
                        log.info("已探测到命令行进程完成标志，等待进程结束...");
                        if (process.waitFor(2, TimeUnit.SECONDS)) {
                            log.info("命令行进程已结束.");
                        } else {
                            log.info("命令行进程执行超时，强制退出.");
                        }
                        break;
                    }
                }
            }
            return true;
        } catch (IOException | InterruptedException e) {
            log.error("命令行执行异常！异常信息：" + e.getMessage(), e);
        } finally {
            if (null != commandLineReader) {
                try {
                    commandLineReader.close();
                } catch (IOException e) {
                    log.error("命令行的输入流资源释放发生异常，异常信息：{}" + e.getMessage());
                }
            }
            if (null != process && process.isAlive()) {
                process.destroyForcibly();
            }
        }
        return false;
    }
}
