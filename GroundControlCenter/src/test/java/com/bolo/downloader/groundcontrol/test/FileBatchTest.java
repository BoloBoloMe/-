package com.bolo.downloader.groundcontrol.test;

import java.io.*;

/**
 * 文件分批处理测试
 */
public class FileBatchTest {
    public static void main(String[] args) throws IOException {
        /* 测试项目1：获取文件大小测试 start */
        File src = new File("D:\\MyResource\\Desktop\\src\\11122020-1 訪王孟源：孟源漫談第一集_今日世界中的假消息（50%版）-wXU7cMapBms.mkv");
        System.out.println(src.length());
        /* 测试项目1：获取文件大小测试 end */


        /* 测试项目2：分批次拷贝文件测试 start */
        File target = new File("D:\\MyResource\\Desktop\\target\\" + src.getName());
        // 通过RandomAccessFile类实现对文件的随机读写 该类不属于IO Stream，相比File类又具与IO Stream接口类似的读写方法，是个很特殊的随机读写工具
        try (RandomAccessFile srcRandomAccesser = new RandomAccessFile(src, "r");
             RandomAccessFile targetRandomAccessFile = new RandomAccessFile(target, "rws")) {
            if (target.exists()) {
                // 跳过已经拷贝过的字节
                srcRandomAccesser.seek(targetRandomAccessFile.length());
                targetRandomAccessFile.seek(targetRandomAccessFile.length());
            } else {
                target.createNewFile();
            }

            // 开始拷贝新字节
            int bufferSize = 1024 * 1024;
            byte[] buffer = new byte[bufferSize];
            int realLength;
            while (true) {
                realLength = srcRandomAccesser.read(buffer, 0, bufferSize);
                if (realLength <= 0) break;
                targetRandomAccessFile.write(buffer, 0, realLength);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


        /* 测试项目2：分批次拷贝文件测试 end */
    }


}
