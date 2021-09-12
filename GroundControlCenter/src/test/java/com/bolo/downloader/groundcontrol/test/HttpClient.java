package com.bolo.downloader.groundcontrol.test;

import org.apache.http.*;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
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
public class HttpClient {
    private static final String URL = "http://127.0.0.1:9000/df";
    private static final CloseableHttpClient client = HttpClientBuilder.create().build();
    private static int currVer = 0;
    private static final byte[] con = new byte[128];
    private static final ResponseHandler<DFResponse> responseHandler = resp -> {
        DFResponse dfResponse = new DFResponse();
        if (Boolean.TRUE.equals(resp.getLastHeader("eq").getValue())) {
            dfResponse.setEqually(true);
            return dfResponse;
        }
        dfResponse.setEqually(false);
        try (InputStream in = resp.getEntity().getContent()) {
            int realLen = in.read(con, 0, con.length);
            dfResponse.setFileSize(Integer.valueOf(resp.getLastHeader("fs").getValue()));
            dfResponse.setFileNane(resp.getLastHeader("fn").getValue());
            dfResponse.setSkip(Long.valueOf(resp.getLastHeader("sp").getValue()));
            dfResponse.setVersion(Integer.valueOf(resp.getLastHeader("vs").getValue()));
            dfResponse.setContent(con, realLen);
            dfResponse.setComplete(true);
        } catch (Exception e) {
            dfResponse.setComplete(false);
            e.printStackTrace();
        }
        return dfResponse;
    };


    public static void main(String[] args) {
        long skip = 0;
        for (int i = 0; i < 10; i++) {
            try {
                DFResponse dfResponse = client.execute(createPostReq(currVer, skip), resp -> {
                    System.out.println(resp.getEntity().toString());
                    DFResponse dfr = new DFResponse();
                    dfr.setEqually(true);
                    return dfr;
                });
                if (dfResponse.isEqually()) {
                    continue;
                }
                if (dfResponse.isComplete() && dfResponse.getFileNane() != null) {
                    File tar = new File("D:\\MyResource\\Desktop\\client\\", dfResponse.getFileNane());
                    if (currVer != dfResponse.getVersion()) {
                        currVer = dfResponse.getVersion();
                        if (!tar.exists()) tar.createNewFile();
                    }
                    try (RandomAccessFile accessFile = new RandomAccessFile(tar, "rws")) {
                        if (accessFile.length() == dfResponse.fileSize) {
                            break;
                        } else if (accessFile.length() > dfResponse.fileSize) {
                            System.err.println("文件数据异常！");
                            break;
                        }
                        accessFile.seek(dfResponse.skip);
                        accessFile.write(dfResponse.getContent());
                        skip = accessFile.length();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (ClientProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println("文件已同步");
        while (true) ;
    }


    private static HttpPost createPostReq(int currVer, long skip) {
        HttpPost request = new HttpPost(URL);
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
        private boolean complete = false;
        private boolean equally = false;
        private String fileNane;
        private int version;
        private int fileSize;
        private long skip;

        private byte[] content;

        public DFResponse() {
        }


        public void setFileNane(String fileNane) {
            this.fileNane = fileNane;
        }

        public void setFileSize(int fileSize) {
            this.fileSize = fileSize;
        }

        public void setContent(byte[] con, int realLen) {
            byte[] content = new byte[realLen];
            System.arraycopy(con, 0, content, 0, realLen);
            this.content = content;
        }

        public boolean isEqually() {
            return equally;
        }

        public void setEqually(boolean equally) {
            this.equally = equally;
        }

        public long getSkip() {
            return skip;
        }

        public int getVersion() {
            return version;
        }

        public void setVersion(int version) {
            this.version = version;
        }

        public void setSkip(long skip) {
            this.skip = skip;
        }

        public boolean isComplete() {
            return complete;
        }

        public void setComplete(boolean complete) {
            this.complete = complete;
        }

        public String getFileNane() {
            return fileNane;
        }

        public int getFileSize() {
            return fileSize;
        }

        public byte[] getContent() {
            return content;
        }
    }


}
