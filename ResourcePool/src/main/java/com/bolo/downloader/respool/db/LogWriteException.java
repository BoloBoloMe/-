package com.bolo.downloader.respool.db;

/**
 * 日志文件写操作出现异常
 */
public class LogWriteException extends RuntimeException {
    public LogWriteException(Throwable cause) {
        super(cause);
    }
}
