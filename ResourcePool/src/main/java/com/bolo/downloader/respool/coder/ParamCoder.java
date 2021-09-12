package com.bolo.downloader.respool.coder;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;

public class ParamCoder {
    private static final Charset charset = Charset.forName("utf8");

    /**
     * 编码
     */
    public static String encode(String name) {
        try {
            name = URLEncoder.encode(name, "utf8");
        } catch (UnsupportedEncodingException e) {
        }
        byte[] bytes = name.getBytes(charset);
        StringBuilder encode = new StringBuilder(name.length());
        for (byte b : bytes) {
            encode.append(b + 128);
        }
        return encode.toString();
    }

    /**
     * 解码
     */
    public static String decode(String name) {
        byte[] bytes = new byte[name.length() / 3];
        int b = 0, n = 0;
        while (b < bytes.length) {
            String tem = name.substring(n, n + 3);
            bytes[b] = (byte) (Integer.parseInt(tem) - 128);
            b++;
            n += 3;
        }
        String decode = new String(bytes, charset);
        try {
            decode = URLDecoder.decode(decode, "utf8");
        } catch (UnsupportedEncodingException e) {
        }
        return decode;
    }

}
