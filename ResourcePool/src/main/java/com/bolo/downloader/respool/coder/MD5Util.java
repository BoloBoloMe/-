package com.bolo.downloader.respool.coder;


import java.io.IOException;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MD5Util {

    /**
     * java获取文件的md5值
     *
     * @param fis 输入流
     * @return
     */
    public static String md5HashCode(RandomAccessFile fis) throws NoSuchAlgorithmException, IOException {
        //拿到一个MD5转换器,如果想使用SHA-1或SHA-256，则传入SHA-1,SHA-256
        MessageDigest md = MessageDigest.getInstance("MD5");
        //分多次将一个文件读入，对于大型文件而言，比较推荐这种方式，占用内存比较少。
        byte[] buffer = new byte[1024];
        int length = -1;
        while ((length = fis.read(buffer, 0, 1024)) != -1) {
            md.update(buffer, 0, length);
        }
        //转换并返回包含16个元素字节数组,返回数值范围为-128到127
        byte[] md5Bytes = md.digest();
        BigInteger bigInt = new BigInteger(1, md5Bytes);//1代表绝对值
        return bigInt.toString(16);//转换为16进制
    }

    /**
     * java计算文件32位md5值
     *
     * @param fis 输入流
     * @return
     */
    public static String md5HashCode32(RandomAccessFile fis) throws NoSuchAlgorithmException, IOException {
        //拿到一个MD5转换器,如果想使用SHA-1或SHA-256，则传入SHA-1,SHA-256
        MessageDigest md = MessageDigest.getInstance("MD5");

        //分多次将一个文件读入，对于大型文件而言，比较推荐这种方式，占用内存比较少。
        byte[] buffer = new byte[1024];
        int length = -1;
        while ((length = fis.read(buffer, 0, 1024)) != -1) {
            md.update(buffer, 0, length);
        }
        //转换并返回包含16个元素字节数组,返回数值范围为-128到127
        byte[] md5Bytes = md.digest();
        StringBuffer hexValue = new StringBuffer();
        for (int i = 0; i < md5Bytes.length; i++) {
            int val = ((int) md5Bytes[i]) & 0xff;//解释参见最下方
            if (val < 16) {
                /**
                 * 如果小于16，那么val值的16进制形式必然为一位，
                 * 因为十进制0,1...9,10,11,12,13,14,15 对应的 16进制为 0,1...9,a,b,c,d,e,f;
                 * 此处高位补0。
                 */
                hexValue.append("0");
            }
            //这里借助了Integer类的方法实现16进制的转换
            hexValue.append(Integer.toHexString(val));
        }
        return hexValue.toString();
    }


    /**
     * java计算文件32位md5值
     *
     * @param fis 输入流
     * @return
     */
    public static String md5HashCode32(RandomAccessFile fis, long len) throws NoSuchAlgorithmException, IOException {
        //拿到一个MD5转换器,如果想使用SHA-1或SHA-256，则传入SHA-1,SHA-256
        MessageDigest md = MessageDigest.getInstance("MD5");

        // 读取指定长度的内容
        for (int l = 0; l < len; l++) {
            md.update(fis.readByte());
        }
        //转换并返回包含16个元素字节数组,返回数值范围为-128到127
        byte[] md5Bytes = md.digest();
        StringBuffer hexValue = new StringBuffer();
        for (int i = 0; i < md5Bytes.length; i++) {
            int val = ((int) md5Bytes[i]) & 0xff;//解释参见最下方
            if (val < 16) {
                /**
                 * 如果小于16，那么val值的16进制形式必然为一位，
                 * 因为十进制0,1...9,10,11,12,13,14,15 对应的 16进制为 0,1...9,a,b,c,d,e,f;
                 * 此处高位补0。
                 */
                hexValue.append("0");
            }
            //这里借助了Integer类的方法实现16进制的转换
            hexValue.append(Integer.toHexString(val));
        }
        return hexValue.toString();
    }

    public static String md5HashCode32(byte[] data) throws NoSuchAlgorithmException {
        //拿到一个MD5转换器,如果想使用SHA-1或SHA-256，则传入SHA-1,SHA-256
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(data);
        //转换并返回包含16个元素字节数组,返回数值范围为-128到127
        byte[] md5Bytes = md.digest();
        StringBuffer hexValue = new StringBuffer();
        for (int i = 0; i < md5Bytes.length; i++) {
            int val = ((int) md5Bytes[i]) & 0xff;//解释参见最下方
            if (val < 16) {
                /**
                 * 如果小于16，那么val值的16进制形式必然为一位，
                 * 因为十进制0,1...9,10,11,12,13,14,15 对应的 16进制为 0,1...9,a,b,c,d,e,f;
                 * 此处高位补0。
                 */
                hexValue.append("0");
            }
            //这里借助了Integer类的方法实现16进制的转换
            hexValue.append(Integer.toHexString(val));
        }
        return hexValue.toString();
    }
}