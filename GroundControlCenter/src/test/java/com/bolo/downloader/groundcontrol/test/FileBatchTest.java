package com.bolo.downloader.groundcontrol.test;

import com.bolo.downloader.respool.coder.MD5Util;

import java.io.*;
import java.security.NoSuchAlgorithmException;

/**
 * 文件分批处理测试
 */
public class FileBatchTest {
    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
        /* 测试项目1：获取文件大小测试 start */
        File src = new File("/home/bolo/Downloads/Bondage-ph5e11af65b2f37.mp4");
        System.out.println(src.length());
        /* 测试项目1：获取文件大小测试 end */


        /* 测试项目2：分批次拷贝文件测试 start */
        File target = new File("/home/bolo/Videos/" + src.getName());
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
            int bufferSize = 1024;
            for (int time = 1; true; time++) {
                byte[] buffer = new byte[bufferSize];
                int realLength;
                realLength = srcRandomAccesser.read(buffer, 0, bufferSize);
                if (realLength <= 0) break;
                targetRandomAccessFile.write(buffer, 0, realLength);
                // 动态调节单次读取的文件内容长度
                if (time % 10 == 0) {
                    bufferSize += 128;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        /* 测试项目2：分批次拷贝文件测试 end */

        /* 测试项目3：两个文件的md5 值是否一致 start */
        String tarMD5 = MD5Util.md5HashCode32(new RandomAccessFile(target, "r"));
        String srcMD5 = MD5Util.md5HashCode32(new RandomAccessFile(src, "r"));
        System.out.println(tarMD5.equals(srcMD5));
        /* 测试项目3：两个文件的md5 值是否一致 end */

    }


}
