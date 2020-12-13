package com.bolo.downloader.respool.test.db.writebuff;

public class IntToChar {
    public static void main(String[] args) {
        int char_max = 65535;
        int max = Integer.MAX_VALUE;
        System.out.println("Integer.MAX_VALUE = " + max);
        // 分解int
        char[] arr = new char[2];
        arr[0] = (char) (max % char_max);
        arr[1] = (char) (max / char_max);
        System.out.println(arr[1] * char_max + arr[0]);
    }
}
