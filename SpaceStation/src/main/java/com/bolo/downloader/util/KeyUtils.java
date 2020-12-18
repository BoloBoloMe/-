package com.bolo.downloader.util;

import com.bolo.downloader.respool.log.LoggerFactory;
import com.bolo.downloader.respool.log.MyLogger;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 密钥工具
 */
public class KeyUtils {
    private static final MyLogger log = LoggerFactory.getLogger(KeyUtils.class);
    private static final ConcurrentHashMap<String, RSAPublicKey> keyMap = new ConcurrentHashMap<>();

    public static RSAPublicKey get(String id) {
        return keyMap.get(id);
    }

    public static RSAPublicKey add(String id, String pc) {
        try {
            //实例化密钥工厂
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            //初始化公钥,根据给定的编码密钥创建一个新的 X509EncodedKeySpec。
            X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(Base64.getDecoder().decode(pc));
            PublicKey publicKey = keyFactory.generatePublic(x509EncodedKeySpec);
            RSAPublicKey rsaPublicKey = (RSAPublicKey) publicKey;
            keyMap.put(id, rsaPublicKey);
            return rsaPublicKey;
        } catch (Exception e) {
            log.error("密钥保存失败！", e);
        }
        return null;
    }
}
