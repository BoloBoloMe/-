package com.bolo.downloader.respool.nio.http.scan;

/**
 * 类路径扫描器，会扫描指定的包路径下的所有类
 */
public class ClasspathScanner implements MethodMapperScanner {
    private String classPath;

    public ClasspathScanner(String classPath) {
        this.classPath = classPath;
    }

    @Override
    public void scan(){

    }
}
