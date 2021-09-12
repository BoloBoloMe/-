package com.bolo.downloader.groundcontrol.test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;

public class LoopFileTest {
    static String NEW_LINE = System.getProperty("line.separator");
    static Charset charset = Charset.forName("utf8");

    public static void main(String[] agrs) throws IOException {
        File logFile = new File("D:\\MyResource\\Desktop\\log\\loopfile.log");
        if (logFile.exists()) logFile.createNewFile();

        try (RandomAccessFile log = new RandomAccessFile(logFile, "rw")) {
            write("key1,value" + NEW_LINE, log);
            write("key2,value" + NEW_LINE, log);
            write("key3,value" + NEW_LINE, log);
            log.seek(0);
            write("key4,value" + NEW_LINE, log);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void write(String content, RandomAccessFile file) throws IOException {
        byte[] con = content.getBytes(charset);
        byte[] len = String.format("%010d", con.length).getBytes(charset);
        // 写入长度
        file.write(len);
        // 写入内容
        file.write(con);
    }

}
