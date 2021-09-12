package com.bolo.downloader.groundcontrol.test;

import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 用netty构建一个HttpClient
 */
public class HttpClient2 {
    private static final String URL = "http://127.0.0.1:9000/test.mp4";
    private static final CloseableHttpClient client = HttpClientBuilder.create().build();


    public static void main(String[] args) {
        try {
            CloseableHttpResponse response = client.execute(createPostReq());
            try (InputStream inputStream = response.getEntity().getContent();
                 OutputStream outputStream = new FileOutputStream("/home/bolo/Videos/Foot fetish-ph583080d6d33f3.mp4")) {
                byte[] buf = new byte[2048];
                int len;
                while (0 < (len = inputStream.read(buf))) {
                    outputStream.write(buf, 0, len);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private static HttpGet createGetReq() {
        HttpGet request = new HttpGet(URL);
        request.setProtocolVersion(new ProtocolVersion("http", 1, 1));
        request.setHeader("connection", "keep-alive");
        RequestConfig.Builder configBuilder = RequestConfig.copy(RequestConfig.DEFAULT);
        configBuilder.setConnectionRequestTimeout(10000);
        request.setConfig(configBuilder.build());
        return request;
    }

    private static HttpPost createPostReq() {
        HttpPost request = new HttpPost(URL);
        request.setProtocolVersion(new ProtocolVersion("http", 1, 1));
        request.setHeader("connection", "keep-alive");
        List<NameValuePair> params = new ArrayList<>();
        request.setEntity(new UrlEncodedFormEntity(params, StandardCharsets.UTF_8));
        RequestConfig.Builder configBuilder = RequestConfig.copy(RequestConfig.DEFAULT);
        configBuilder.setConnectionRequestTimeout(10000);
        request.setConfig(configBuilder.build());
        return request;
    }
}
