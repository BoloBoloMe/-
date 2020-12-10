package com.bolo.downloader.groundcontrol;


import com.bolo.downloader.groundcontrol.factory.ConfFactory;
import com.bolo.downloader.groundcontrol.factory.StoneMapFactory;
import com.bolo.downloader.respool.db.StoneMap;
import com.bolo.downloader.respool.log.LoggerFactory;
import com.bolo.downloader.respool.log.MyLogger;
import org.apache.http.NameValuePair;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;

import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class Cantor {
    public static final String CONF_FILE_PATH = "/home/bolo/program/VideoDownloader_GroundControlCenter/conf/GroundControlCenter.conf";
    //    public static final String CONF_FILE_PATH = "";
    private static final MyLogger log = LoggerFactory.getLogger(Cantor.class);

    static private final int INIT_EXPECTED_LEN = 1024;
    static private final String lastVerKey = "lastVer";
    private static final ResponseHandler<DFResponse> RESPONSE_HANDLER = resp -> {
        DFResponse dfResponse = new DFResponse();
        if (resp.getStatusLine().getStatusCode() / 100 != 2) {
            throw new RuntimeException("服务器返回操作失败响应码：" + resp.getStatusLine().getStatusCode());
        }
        try (InputStream in = resp.getEntity().getContent()) {
            dfResponse.setStatus(Integer.parseInt(resp.getLastHeader("st").getValue()));
            dfResponse.setFileNane(resp.getLastHeader("fn") == null ? "" : URLDecoder.decode(resp.getLastHeader("fn").getValue(), "utf8"));
            dfResponse.setSkip(Long.parseLong(resp.getLastHeader("sp").getValue()));
            dfResponse.setVersion(Integer.parseInt(resp.getLastHeader("vs").getValue()));
            if (dfResponse.getStatus() == 2) {
                int conLen = Integer.parseInt(resp.getLastHeader("content-length").getValue());
                byte[] content = new byte[conLen];
                in.read(content, 0, conLen);
                dfResponse.setContent(content);
            }
            dfResponse.setComplete(true);
        } catch (Exception e) {
            dfResponse.setComplete(false);
            log.error("服务器响应解析失败！", e);
        }
        return dfResponse;
    };

    static {
        LoggerFactory.setLogPath(ConfFactory.get("logPath"));
        LoggerFactory.setLogFileName(ConfFactory.get("logFileName"));
        LoggerFactory.roll();
    }

    public static void main(String[] args) {
        // 客户端状态，当状态发生变化，需要打印日志
        int clientStatus = -1;
        // 单次请求的文件内容长度
        int expectedLen = INIT_EXPECTED_LEN;
        // 每10次请求读超时数
        int readTimeoutCount = 0;
        // stoneMap组成：lastVerKey->lastVer; lastVer->lastFileName, lastFileName->skip
        final StoneMap map = StoneMapFactory.getObject();
        int lastVer = 0;
        long skip = 0;
        String lastFileName = null;
        if (null != map.get(lastVerKey) && (lastVer = Integer.valueOf(map.get(lastVerKey))) > 0) {
            lastFileName = map.get(map.get(lastVerKey));
            skip = Long.valueOf(map.get(lastFileName));
        }
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            File tar = null;
            if (lastFileName != null) tar = new File(ConfFactory.get("filePath"), lastFileName);
            // synchronous loop
            for (int time = 1; ; time++) {
                try {
                    DFResponse dfResponse = client.execute(createPostReq(lastVer, skip, expectedLen), RESPONSE_HANDLER);
                    if (!dfResponse.isComplete()) {
                        log.error("服务器响应不可用，重新请求");
                        sleep(1000);
                        continue;
                    }
                    if (dfResponse.getStatus() == 0) {
                        if (clientStatus != 0) {
                            log.info("status: equals");
                            clientStatus = 0;
                        }
                        // 数据一致，继续检查
                        sleep(10000);
                        continue;
                    }
                    if (dfResponse.getStatus() == 1 || dfResponse.getStatus() == 3) {
                        if (clientStatus != 1) {
                            log.info("status: create new file：%s", dfResponse.getFileNane());
                            clientStatus = 1;
                        } else {
                            log.error("服务器连续返回新增文件响应！产生空文件：" + tar.getName());
                        }
                        // 有新的文件，创建文件
                        map.put(lastVerKey, Integer.toString(lastVer = dfResponse.getVersion()));
                        map.put(map.get(lastVerKey), dfResponse.getFileNane());
                        map.put(dfResponse.getFileNane(), "0");
                        tar = new File(ConfFactory.get("filePath"), dfResponse.getFileNane());
                        if (tar.exists()) tar.delete();
                        tar.createNewFile();
                        skip = tar.length();
                        // 重置预期文件内容长度
                        expectedLen = INIT_EXPECTED_LEN;
                        continue;
                    }
                    if (dfResponse.getStatus() == 2) {
                        log.info("当前流量: %d byte.", dfResponse.getContent().length);
                        if (clientStatus != 2) {
                            log.info("status: write file: %s", tar.getName());
                            clientStatus = 2;
                        }
                        // 当前文件有数据需要继续写入
                        try (RandomAccessFile accessFile = new RandomAccessFile(tar, "rws")) {
                            accessFile.seek(dfResponse.skip);
                            accessFile.write(dfResponse.getContent());
                            skip = accessFile.length();
                            map.put(tar.getName(), Long.toString(skip));
                        } catch (Exception e) {
                            log.error("文件写入失败！fileName：" + tar.getName(), e);
                        }
                    }
                } catch (Exception e) {
                    log.error("服务器请求失败！", e);
                    if (e.getMessage().contains("Read time out")) {
                        readTimeoutCount++;
                    }
                } finally {
                    if (map.modify() < 10) {
                        map.flushWriteBuff();
                    } else {
                        map.rewriteDbFile();
                    }
                }

                // 动态调整单次请求的内容长度,每10次写请求作一次调整
                if (clientStatus == 2 && time % 10 == 0) {
                    if (readTimeoutCount >= 5) {
                        // 有半数及以上请求超时,调低单次请求的文件内容长度,最低 128 byte
                        if (expectedLen > 128) {
                            expectedLen -= 128;
                        }
                    } else {
                        // 有过半请求未超时,调高单次请求的文件内容长度,最高 1m
                        if (expectedLen < 1048576) {
                            expectedLen += 128;
                        }
                    }
                    // 重置超时统计
                    readTimeoutCount = 0;
                }
            }
        } catch (IOException e) {
            log.error("http 客户端创建失败！", e);
        }
    }


    private static HttpPost createPostReq(int currVer, long skip, int expectedLen) {
        HttpPost request = new HttpPost(ConfFactory.get("url"));
        request.setProtocolVersion(new ProtocolVersion("http", 1, 1));
        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("cv", Integer.toString(currVer)));
        params.add(new BasicNameValuePair("sp", Long.toString(skip)));
        params.add(new BasicNameValuePair("el", Integer.toString(expectedLen)));
        request.setEntity(new UrlEncodedFormEntity(params, StandardCharsets.UTF_8));
        request.setHeader("content-type", "text/plain; charset=UTF-8");
        request.setHeader("connection", "keep-alive");
        RequestConfig.Builder configBuilder = RequestConfig.copy(RequestConfig.DEFAULT);
        configBuilder.setConnectionRequestTimeout(10000);
        request.setConfig(configBuilder.build());
        return request;
    }

    private static class DFResponse {
        // 0-数据一致态; 1-文件新增态，存在新的文件，fileName 存放着新文件名; 2-文件同步态，content保存着当前文件的新内容; 3-文件已遗失;
        private int status;
        private String fileNane;
        private int version;
        private long skip;

        private byte[] content;

        private boolean complete;

        public void setContent(byte[] content) {
            this.content = content;
        }

        public int getStatus() {
            return status;
        }

        public void setStatus(int status) {
            this.status = status;
        }

        public String getFileNane() {
            return fileNane;
        }

        public void setFileNane(String fileNane) {
            this.fileNane = fileNane;
        }

        public int getVersion() {
            return version;
        }

        public void setVersion(int version) {
            this.version = version;
        }

        public long getSkip() {
            return skip;
        }

        public void setSkip(long skip) {
            this.skip = skip;
        }

        public byte[] getContent() {
            return content;
        }

        public boolean isComplete() {
            return complete;
        }

        public void setComplete(boolean complete) {
            this.complete = complete;
        }
    }

    private static void sleep(long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
        }
    }
}
