package com.bolo.downloader.respool.db.buff;

/**
 * 日志文件写操作出现异常
 */
public class LogReadException extends RuntimeException {
    private String message;

    public LogReadException(Throwable cause) {
        super(cause);
        this.message = cause.getMessage();
    }

    public LogReadException(String message) {
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
