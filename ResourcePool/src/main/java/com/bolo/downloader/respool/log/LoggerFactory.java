package com.bolo.downloader.respool.log;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.*;

public class LoggerFactory {
    private static String logPath;
    private static String logFileName;
    private static String lastChangeLogFileDate;
    private static long lastRollTime = 0;

    private static MyLogger myLogger = null;

    public static MyLogger getLogger() {
        return null != myLogger ? myLogger : createSingleton();
    }

    private static synchronized MyLogger createSingleton() {
        if (myLogger == null) {
            return myLogger = new MyLogger(newLogger());
        }
        return myLogger;
    }

    private static Logger newLogger() {
        try {
            lastChangeLogFileDate = new SimpleDateFormat("YYYY-MM-dd").format(new Date());
            logFileName = lastChangeLogFileDate + '_' + logFileName;
            Logger log = Logger.getLogger(logFileName);
            FileHandler handler = new FileHandler(logPath + logFileName);
            handler.setFormatter(new LogFormatter());
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
                    return String.format("%s [ERROR] [thread-id:%d]: %s %s", dateFormat.format(new Date(record.getMillis())), record.getThreadID(),  record.getMessage(), lineSeparator);
                } else {
                    return String.format("%s [ERROR] [thread-id:%d]: %s %s exception message:%s,%s",
                            dateFormat.format(new Date(record.getMillis())), record.getThreadID(), record.getMessage(), lineSeparator, exception.getMessage(), getStackTrace(exception, lineSeparator));
                }
            }
            return String.format("%s [INFO] [thread-id:%d]: %s %s", dateFormat.format(new Date(record.getMillis())), record.getThreadID(),  record.getMessage(), lineSeparator);
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
        if (System.currentTimeMillis() - lastRollTime < 120000) {
            return;
        } else {
            lastRollTime = System.currentTimeMillis();
        }
        String currDate = new SimpleDateFormat("YYYY-MM-dd").format(new Date());
        if (currDate.equals(lastChangeLogFileDate)) {
            return;
        }
        lastChangeLogFileDate = currDate;
        myLogger.setLogger(newLogger());
    }
}
