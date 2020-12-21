package com.bolo.downloader.groundcontrol.test;

public class StringTest {
    public static void main(String[] args) {
        String range = "bytes=0-";
        String regex = "bytes=[0-9]+-[1-9][0-9]*";
        System.out.println(range.matches(regex));
        final long start, end;
        if (range.matches(regex)) {
            int index_0 = range.indexOf('=') + 1, index_1 = range.indexOf('-');
            start = Long.parseLong(range.substring(index_0, index_1));
            end = Long.parseLong(range.substring(index_1 + 1));
        } else {
            start = 0;
            end = range.length() - 1;
        }
        final long transLen = end - start;
        System.out.println(String.format("%d,%d,%d", start, end, transLen));
    }
}
