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
//    public static final String CONF_FILE_PATH = "D:\\MyResource\\Desktop\\conf\\GroundControlCenter.conf";
        public static final String CONF_FILE_PATH = "";
    private static final MyLogger log = LoggerFactory.getLogger(Cantor.class);

    static private final String lastVerKey = "lastVer";
    private static final byte[] con = new byte[256];
    private static final ResponseHandler<DFResponse> RESPONSE_HANDLER = resp -> {
        DFResponse dfResponse = new DFResponse();
        if (resp.getStatusLine().getStatusCode() / 100 != 2) {
            throw new RuntimeException("服务器返回操作失败响应码：" + resp.getStatusLine().getStatusCode());
        }
        try (InputStream in = resp.getEntity().getContent()) {
            dfResponse.setStatus(Integer.valueOf(resp.getLastHeader("st").getValue()));
            dfResponse.setFileNane(resp.getLastHeader("fn") == null ? "" : URLDecoder.decode(resp.getLastHeader("fn").getValue(), "utf8"));
            dfResponse.setSkip(Long.valueOf(resp.getLastHeader("sp").getValue()));
            dfResponse.setVersion(Integer.valueOf(resp.getLastHeader("vs").getValue()));
            if (dfResponse.getStatus() == 2) {
                int realLen = in.read(con, 0, con.length);
                dfResponse.setContent(con, realLen);
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
        // stoneMap组成：lastVerKey->lastVer; lastVer->lastFileName, lastFileName->skip
        int clientStatus = -1;
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
            while (true) {
                try {
                    DFResponse dfResponse = client.execute(createPostReq(lastVer, skip), RESPONSE_HANDLER);
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
                        }
                        // 有新的文件，创建文件
                        map.put(lastVerKey, Integer.toString(lastVer = dfResponse.getVersion()));
                        map.put(map.get(lastVerKey), dfResponse.getFileNane());
                        map.put(dfResponse.getFileNane(), "0");
                        tar = new File(ConfFactory.get("filePath"), dfResponse.getFileNane());
                        if (tar.exists()) tar.delete();
                        tar.createNewFile();
                        skip = tar.length();
                        continue;
                    }
                    if (dfResponse.getStatus() == 2) {
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
                } finally {
                    if (map.modify() < 10) {
                        map.flushWriteBuff();
                    } else {
                        map.rewriteDbFile();
                    }
                }
            }
        } catch (IOException e) {
            log.error("http 客户端创建失败！", e);
        }
    }


    private static HttpPost createPostReq(int currVer, long skip) {
        HttpPost request = new HttpPost(ConfFactory.get("url"));
        request.setProtocolVersion(new ProtocolVersion("http", 1, 1));
        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("cv", Integer.toString(currVer)));
        params.add(new BasicNameValuePair("sp", Long.toString(skip)));
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

        public void setContent(byte[] con, int realLen) {
            byte[] content = new byte[realLen];
            System.arraycopy(con, 0, content, 0, realLen);
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
