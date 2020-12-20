package com.bolo.downloader.respool.test.db.coder;

import com.bolo.downloader.respool.coder.ras.RSAUtils;

import java.util.Arrays;

public class RSAUtilsTest {
    public static void main(String[] args) throws Exception {
            RSAUtils.KeyHolder key = RSAUtils.genKey();
            System.out.println("公钥：" + key.publicKeyString());
            System.out.println("私钥：" + key.privateKeyString());
        for (int i = 1; i <= 65535; i++) {
            byte[] data = new byte[i];
            Arrays.fill(data, (byte) 99);
            // 加密
            byte[] encodeData = RSAUtils.encode(data, data.length, key.publicKeyString());
            System.out.println("原长度：" + data.length);
            System.out.println("加密后长度：" + encodeData.length);
            // 解密
            byte[] decodeData = RSAUtils.decode(encodeData, encodeData.length, key.privateKeyString());
            assert Arrays.equals(data, decodeData) : "解密后的数据与原数据不一致";
            System.out.println("解密后的数据与原数据一致.");
        }
    }
}
