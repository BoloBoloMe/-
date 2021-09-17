package com.bolo.downloader.respool.nio.http.scan;

/**
 * Controller 方法映射扫描器，会根据特定方式扫 @Controller 注释的类并为其中有 @RequestMapping 注释的方法创建 MethodMapper 对象
 */
public interface MethodMapperScanner {
    void scan();
}
