package com.bolo.downloader.respool.coder;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;

/**
 * 使用JDK自带的工具，基于RSA算法的加解密工具类
 */
public class RSAUtils {

    static private final String RSA_ALGORITHM = "RSA";

    /**
     * 创建新的密钥对
     *
     * @throws NoSuchAlgorithmException
     */
    public static KeyHolder genKey() throws NoSuchAlgorithmException {
        //KeyPairGenerator用于生成公钥和私钥对。密钥对生成器是使用 getInstance 工厂方法
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(RSA_ALGORITHM);
        //密钥长度，DSA算法的默认密钥长度是1024，必须是64的倍数，在512到65536位之间
        keyPairGenerator.initialize(1024);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        return new KeyHolder(publicKey, privateKey);
    }

    /**
     * 加密
     *
     * @param data 需要被加密的数据
     * @param len  data中需要被读取的长度
     * @param pc   公钥的Base64编码字符串
     * @return 加密后的数据
     * @throws Exception
     */
    public static byte[] encode(final byte[] data, int len, String pc) throws Exception {
        //实例化密钥工厂
        KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
        //初始化公钥,根据给定的编码密钥创建一个新的 X509EncodedKeySpec。
        X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(Base64.getDecoder().decode(pc));
        PublicKey publicKey = keyFactory.generatePublic(x509EncodedKeySpec);
        return encode(data, len, publicKey);
    }

    /**
     * 加密
     *
     * @param data      需要被加密的数据
     * @param len       data中可读取的长度
     * @param publicKey 公钥
     * @return 加密后的数据
     * @throws Exception
     */
    public static byte[] encode(final byte[] data, int len, PublicKey publicKey) throws Exception {
        Cipher cipher = Cipher.getInstance(RSA_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        return batchAct(cipher, data, len, true);
    }


    /**
     * 解密
     *
     * @param data 需要被解密的数据
     * @param len  data中可内读取的长度
     * @param pr   私钥的Base64字符串
     * @return 解密后的数据
     * @throws Exception
     */
    public static byte[] decode(final byte[] data, int len, String pr) throws Exception {
        //取得私钥
        PKCS8EncodedKeySpec pkcs8KeySpec = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(pr));
        KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
        //生成私钥
        PrivateKey privateKey = keyFactory.generatePrivate(pkcs8KeySpec);
        return decode(data, len, privateKey);
    }


    /**
     * 加/解密共用逻辑：由于RSA加密算法单次可处理的byte数组长度是根据key长度而确定的，如果需要加密的数数据量太大就分解成多次进行加解密运算，并在最后汇总成一个结果
     *
     * @param cipher  进行加解密运算的工具类
     * @param data    需要加解密的总数据
     * @param dataLen data中可读的长度
     * @param encode  true-执行加密运算，false-执行解密运算
     * @return
     * @throws BadPaddingException
     * @throws IllegalBlockSizeException
     */
    private static byte[] batchAct(Cipher cipher, byte[] data, int dataLen, boolean encode) throws BadPaddingException, IllegalBlockSizeException {
        // 单次从data中读取的长度; 单次运算可获得的长度; 加密/解密次数; 数据长度与单次读长度的模
        final int readSize, writeSize, count, mod;
        if (encode) {
            // 加密
            if (dataLen > 117) {
                readSize = 117;
                mod = dataLen % readSize;
                count = dataLen / readSize + (mod > 0 ? 1 : 0);
            } else {
                readSize = dataLen;
                count = 1;
                mod = 0;
            }
            writeSize = 128;
        } else {
            // 解密
            if (dataLen < 128) {
                throw new BadPaddingException("需要解密的数据量低于有可能的最小值");
            }
            readSize = 128;
            if (dataLen == 128) {
                count = 1;
                mod = 0;
            } else {
                mod = dataLen % readSize;
                count = dataLen / readSize + (mod > 0 ? 1 : 0);
            }
            writeSize = 117;
        }

        byte[] operator = new byte[readSize];
        byte[] result = new byte[count * writeSize];
        int writeLenCount = 0;
        for (int i = 0; i < count; i++) {
            int readLength;
            if (readSize == dataLen) {
                readLength = readSize;
            } else {
                // 进到这里表示 readSize < dataLen
                if (i == count - 1) {
                    readLength = 0 == mod ? readSize : mod;
                } else {
                    readLength = readSize;
                }
            }
            System.arraycopy(data, i * readSize, operator, 0, readLength);
            byte[] finalArr = cipher.doFinal(readLength == readSize ? operator : Arrays.copyOfRange(operator, 0, readLength));
            System.arraycopy(finalArr, 0, result, i * writeSize, finalArr.length);
            writeLenCount += finalArr.length;
        }
        return writeLenCount == result.length ? result : Arrays.copyOfRange(result, 0, writeLenCount);
    }

    /**
     * 解密
     *
     * @param data       需要被解密的数据
     * @param len        data中可内读取的长度
     * @param privateKey 私钥
     * @return 解密后的数据
     */
    public static byte[] decode(byte[] data, int len, PrivateKey privateKey) throws Exception {
        Cipher cipher = Cipher.getInstance(RSA_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        return batchAct(cipher, data, len, false);
    }

    public static class KeyHolder {
        private RSAPublicKey publicKey;
        private RSAPrivateKey privateKey;
        private String publicKeyString;
        private String privateKeyString;
        private byte[] publicKeyByte;
        private byte[] privateKeyByte;

        public KeyHolder(RSAPublicKey publicKey, RSAPrivateKey privateKey) {
            this.publicKey = publicKey;
            this.privateKey = privateKey;
            this.publicKeyByte = publicKey.getEncoded();
            this.privateKeyByte = privateKey.getEncoded();
            this.publicKeyString = Base64.getEncoder().encodeToString(this.publicKeyByte);
            this.privateKeyString = Base64.getEncoder().encodeToString(this.privateKeyByte);
        }

        public RSAPrivateKey getPrivateKey() {
            return privateKey;
        }

        public RSAPublicKey getPublicKey() {
            return publicKey;
        }

        public String privateKeyString() {
            return privateKeyString;
        }

        public String publicKeyString() {
            return publicKeyString;
        }

        public byte[] privateKeyByte() {
            return privateKeyByte;
        }

        public byte[] publicKeyByte() {
            return publicKeyByte;
        }
    }
}
