package com.bolo.downloader.respool.log;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.*;

public class LoggerFactory {
    private static String logPath;
    private static String logFileName;

    public static MyLogger getLogger(Class clazz) {
        return new MockLogger(clazz);
    }

    private static Logger newLogger() {
        try {
            File pathFile = new File(logPath);
            if (!pathFile.exists() && !pathFile.mkdirs()) throw new Error("日志目录创建失败！");
            String currFileName = logPath + new SimpleDateFormat("YYYY-MM-dd_HHmmss").format(new Date()) + '_' + logFileName;
            File logFile = new File(currFileName);
            if (!logFile.exists() && !logFile.createNewFile()) throw new Error("日志文件创建失败！");
            Logger log = Logger.getLogger(currFileName);
            FileHandler handler = new FileHandler(currFileName);
            handler.setFormatter(new LogFormatter());
            handler.setEncoding("utf-8");
            log.addHandler(handler);
            return log;
        } catch (IOException e) {
            throw new Error(e);
        }
    }

    private static class LogFormatter extends Formatter {
        private final DateFormat dateFormat = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss:SSS");

        public String format(LogRecord record) {
            String lineSeparator = System.lineSeparator();
            if (record.getLevel() == Level.WARNING) {
                Throwable exception = record.getThrown();
                if (exception == null) {
                    return String.format("%s [ERROR] [thread-id:%d]: %s %s", dateFormat.format(new Date(record.getMillis())), record.getThreadID(), record.getMessage(), lineSeparator);
                } else {
                    return String.format("%s [ERROR] [thread-id:%d]: %s %s exception message:%s,%s",
                            dateFormat.format(new Date(record.getMillis())), record.getThreadID(), record.getMessage(), lineSeparator, exception.getMessage(), getStackTrace(exception, lineSeparator));
                }
            }
            return String.format("%s [INFO] [thread-id:%d]: %s %s", dateFormat.format(new Date(record.getMillis())), record.getThreadID(), record.getMessage(), lineSeparator);
        }

        private String getStackTrace(Throwable exception, String lineSeparator) {
            StringBuilder buff = new StringBuilder();
            while (true) {
                StackTraceElement[] elements = exception.getStackTrace();
                for (StackTraceElement element : elements) {
                    buff.append("at: ").append(element.toString()).append(lineSeparator);
                }
                if (null != (exception = exception.getCause())) {
                    continue;
                } else {
                    break;
                }
            }
            return buff.toString();
        }
    }

    public static void setLogPath(String logPath) {
        LoggerFactory.logPath = logPath;
    }

    public static void setLogFileName(String logFileName) {
        LoggerFactory.logFileName = logFileName;
    }

    /**
     * 创建新日期下的日志对象
     */
    public static void roll() {
        SuperLogger.setLogger(newLogger());
    }
}
