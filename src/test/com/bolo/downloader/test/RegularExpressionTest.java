package com.bolo.downloader.test;

import org.junit.Test;

import java.util.regex.Pattern;

public class RegularExpressionTest {
    @Test
    public void distinguishVideoFile() {
        String[] fileNames = {"aaa.mp4","aaa.avi","aaa.jar"};
        for (String fileName : fileNames) {
            System.out.println(fileName + "::::::" + Pattern.matches(".+(\\.mp4|\\.avi){1}", fileName));
        }
    }
}
