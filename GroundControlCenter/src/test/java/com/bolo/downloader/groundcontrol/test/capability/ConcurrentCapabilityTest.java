package com.bolo.downloader.groundcontrol.test.capability;

import com.bolo.downloader.groundcontrol.factory.HttpClientFactory;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.*;
import java.util.concurrent.*;

/**
 * 并发性能测试
 */
public class ConcurrentCapabilityTest {
    private static final String url = "http://www.gcc.net:9999";
    private static final int threadCount = 300;
    private static final CountDownLatch latchStart = new CountDownLatch(threadCount);
    private static final CountDownLatch latchEnd = new CountDownLatch(threadCount);
    private static final ConcurrentLinkedDeque<String> deque = new ConcurrentLinkedDeque<>();
    private static final ConcurrentHashMap<String, Long> reqTimeHolder = new ConcurrentHashMap<>();
    private static volatile long millisecond = System.currentTimeMillis();

    public static void main(String[] args) throws IOException {
        Timer timer = new Timer("timer", true);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                millisecond = System.currentTimeMillis();
            }
        }, 0L, 1000L);

        CloseableHttpClient mainClient = HttpClientFactory.http();
        String[] fileNames = mainClient.execute(get(url + "/fl"), response -> {
            final long contentLength = Long.parseLong(response.getLastHeader("content-length").getValue());
            HttpEntity entity = response.getEntity();
            String fileListJson;
            try (InputStream in = entity.getContent();
                 ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                byte[] dataCache = new byte[1024];
                int readCount = 0;
                int readLen = 0;
                while (readCount < contentLength) {
                    readLen = in.read(dataCache, 0, 1024);
                    out.write(dataCache, 0, readLen);
                    readCount += readLen;
                }
                byte[] data = out.toByteArray();
                fileListJson = new String(data, "utf8");
            }
            return fileListJson.substring(1, fileListJson.length() - 1).replaceAll("\"", "").split(",");
        });
        for (String name : fileNames) {
            deque.add(URLEncoder.encode(name, "utf8"));
        }
        System.out.println("数据准备完毕.");
        System.out.println("文件数=" + deque.size());
        System.out.println("并发线程数量=" + threadCount);
        for (int i = 1; i <= threadCount; i++) {
            Thread thread = new Thread(() -> {
                CloseableHttpClient client = HttpClientFactory.http();
                latchStart.countDown();
                System.out.println("线程" + Thread.currentThread().getName() + "准备完毕,等待任务开启");
                awaitLatch(latchStart);
                for (; ; ) {
                    String name = deque.poll();
                    if (name == null) {
                        break;
                    }
                    HttpGet req = get(url + "/pl?tar=" + name);
                    try {
                        final long reqTime = millisecond;
                        client.execute(req, response -> {
                            int code = response.getStatusLine().getStatusCode();
                            if (code < 200 || code > 299) {
                                return response;
                            }
                            String contentRange = response.getLastHeader("content-range").getValue();
                            long fileSize = Long.parseLong(contentRange.substring(contentRange.lastIndexOf('/') + 1));
                            try (InputStream in = response.getEntity().getContent()) {
                                long count = 0;
                                while (count >= fileSize) {
                                    count += in.read();
                                }
                            }
                            final long respTime = millisecond;
                            reqTimeHolder.put(Thread.currentThread().getId() + "_" + millisecond, respTime - reqTime);
                            return response;
                        });
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                latchEnd.countDown();
            });
            thread.setName("work_thread_" + i);
            thread.start();
        }
        sleet();
        awaitLatch(latchStart);
        final long startTime = System.currentTimeMillis();
        System.out.println("开始并发获取文件流...");
        awaitLatch(latchEnd);
        final long endTime = System.currentTimeMillis();
        System.out.println("获取完毕.总耗时=" + (endTime - startTime));
        System.out.println("成功获取所有数据的线程总数=" + reqTimeHolder.size());
        System.out.println("平均用时=" + agvTime());
    }


    public static void awaitLatch(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void sleet() {
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static HttpGet get(String url) {
        HttpGet request = new HttpGet(url);
        request.setProtocolVersion(new ProtocolVersion("http", 1, 1));
        List<NameValuePair> params = new ArrayList<>();
        request.setHeader("content-type", "text/plain; charset=UTF-8");
        request.setHeader("connection", "keep-alive");
//        request.setHeader("range", "bytes=" + skip + "-");
        RequestConfig.Builder configBuilder = RequestConfig.copy(RequestConfig.DEFAULT);
        // 设置请求和传输超时
        configBuilder.setSocketTimeout(3000).setConnectionRequestTimeout(3000);
        request.setConfig(configBuilder.build());
        return request;
    }

    public static long agvTime() {
        long count = 0;
        Set<Map.Entry<String, Long>> entrySet = reqTimeHolder.entrySet();
        for (Map.Entry<String, Long> entry : entrySet) {
            count += entry.getValue();
        }
        return count / reqTimeHolder.size();
    }
}
