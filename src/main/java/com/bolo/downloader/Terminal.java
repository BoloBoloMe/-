package com.bolo.downloader;


import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

@Slf4j
public class Terminal {
    private static final String IDIOMATIC = "java@localhost:$  {}";

    private static final String flag = "[download] 100% ";

    public static boolean execYoutubeDL(String url, String filePath) {
        return exec("youtube-dl " + url, flag);
    }

    private static boolean exec(String command, String flag) {
        boolean result = false;
        Process process = null;
        BufferedReader commandLineReader = null;
        BufferedReader errorReader = null;
        try {
            log.info(IDIOMATIC, command);
            process = Runtime.getRuntime().exec(command);
            commandLineReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            String export;
            while ((export = commandLineReader.readLine()) != null) {
                log.info(IDIOMATIC, export);
                if (export.contains(flag)) {
                    log.info("已探测到命令行进程成功标志");
                    result = true;
                }
            }
            while ((export = errorReader.readLine()) != null) {
                log.error(IDIOMATIC, export);
            }
            log.info("命令行执行完毕.");
        } catch (IOException e) {
            log.error("命令行执行异常！异常信息：" + e.getMessage(), e);
        } finally {
            if (null != commandLineReader) {
                try {
                    commandLineReader.close();
                } catch (IOException e) {
                    log.error("命令行的输入流资源释放发生异常，异常信息：{}" + e.getMessage());
                }
            }
            if (null != errorReader) {
                try {
                    errorReader.close();
                } catch (IOException e) {
                    log.error("命令行的异常输入流资源释放发生异常，异常信息：{}" + e.getMessage());
                }
            }
            if (null != process && process.isAlive()) {
                process.destroyForcibly();
            }
        }
        return result;
    }
}