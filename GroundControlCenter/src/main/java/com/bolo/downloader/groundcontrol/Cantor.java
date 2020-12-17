package com.bolo.downloader.groundcontrol;


import com.bolo.downloader.groundcontrol.factory.ConfFactory;
import com.bolo.downloader.groundcontrol.factory.HttpClientFactory;
import com.bolo.downloader.groundcontrol.factory.StoneMapFactory;
import com.bolo.downloader.respool.coder.MD5Util;
import com.bolo.downloader.respool.db.StoneMap;
import com.bolo.downloader.respool.log.LoggerFactory;
import com.bolo.downloader.respool.log.MyLogger;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class Cantor {
    private static final String CONF_FILE_PATH = "conf/GroundControlCenter.conf";
    static private final MyLogger log = LoggerFactory.getLogger(Cantor.class);
    static private final String KEY_LAST_VER = "LV";
    static private final String KEY_LAST_FILE = "LF";
    static private final String KEY_FILE_STATE = "FS";
    // 文件状态：已下载
    static private final String VAL_FILE_STATE_DOWNLOAD = "DOWNLOAD";
    // 文件状态：新文件
    static private final String VAL_FILE_STATE_NEW = "NEW";

    static private final ResponseHandler<DFResponse> RESPONSE_COVER = resp -> {
        DFResponse dfResponse = new DFResponse();
        if (resp.getStatusLine().getStatusCode() / 200 != 1) {
            throw new RuntimeException("服务器返回操作失败响应码：" + resp.getStatusLine().getStatusCode());
        }
        dfResponse.setStatus(Integer.parseInt(resp.getLastHeader("st").getValue()));
        dfResponse.setVersion(Integer.parseInt(resp.getLastHeader("vs").getValue()));
        if (dfResponse.getStatus() == 2) {
            dfResponse.setEntity(resp.getEntity());
        }
        Header fn = resp.getLastHeader("fn");
        if (fn == null || "".equals(fn.getValue())) {
            dfResponse.setFileNane(null);
        } else if (dfResponse.getStatus() == 4) {
            dfResponse.setFileNane(fn.getValue());
        } else {
            dfResponse.setFileNane(URLDecoder.decode(fn.getValue(), "utf8"));
        }
        dfResponse.setComplete(true);
        return dfResponse;
    };

    public static void main(String[] args) {
        init();
        // StoneMap键值对关系：lastVerKey->lastVer; lastVer->lastFileName; lastFileName-> isDone
        final StoneMap map = StoneMapFactory.getObject();
        String filePath = ConfFactory.get("filePath");
        int clientStatus = -1, expectedValue, lastVer;
        long skip;
        String lastFileName;
        if (null != map.get(KEY_LAST_VER) && null != map.get(KEY_LAST_FILE) && null != map.get(KEY_FILE_STATE)) {
            // StoneMap 保存着进程关闭前的状态，进行读取
            lastVer = Integer.parseInt(map.get(KEY_LAST_VER));
            lastFileName = map.get(KEY_LAST_FILE);
            if (VAL_FILE_STATE_DOWNLOAD.equals(map.get(KEY_FILE_STATE))) {
                // 上次关闭前下载的文件已经完成下载，通知服务器清除文件
                expectedValue = 0;
                skip = 0;
            } else {
                // 上次关闭前下载的文件尚未完成下载，向服务器请求文件内容
                File tar = new File(filePath, lastFileName);
                skip = tar.exists() ? tar.length() : 0L;
                expectedValue = 1;
            }
        } else {
            // StoneMap 中没有保存有效信息，初始化各变量
            lastVer = 0;
            skip = 0;
            expectedValue = 1;
            lastFileName = "null";
            map.clear();
        }
        try (CloseableHttpClient client = HttpClientFactory.http()) {
            log.info("客户端启动成功.");
            log.info("当前版本号: %d", lastVer);
            log.info("访问的服务器地址: %s", ConfFactory.get("url"));
            for (int time = 1; ; time++) {
                try {
                    DFResponse dfResponse = client.execute(createPostReq(lastVer, skip, expectedValue), RESPONSE_COVER);
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
                        // 有新的文件
                        if (clientStatus != 1) {
                            log.info("status: create new file：%s", dfResponse.getFileNane());
                            clientStatus = 1;
                        } else {
                            log.error("服务器连续返回新增文件响应！产生空文件：" + lastFileName);
                        }
                        map.put(KEY_LAST_VER, Integer.toString(dfResponse.getVersion()));
                        map.put(KEY_LAST_FILE, dfResponse.getFileNane());
                        map.put(KEY_FILE_STATE, VAL_FILE_STATE_NEW);
                        // 下载文件
                        download(client, dfResponse, new File(filePath, dfResponse.fileNane));
                        // 重置请求参数
                        lastVer = dfResponse.getVersion();
                        expectedValue = 1;
                        skip = 0;
                        continue;
                    }
                    if (dfResponse.getStatus() == 2) {
                        // 当前文件有数据需要继续写入
                        if (clientStatus != 2) {
                            log.info("status: write file: %s", dfResponse.getFileNane());
                            clientStatus = 2;
                        }
                        File tar = new File(filePath, dfResponse.fileNane);
                        if (!tar.exists()) tar.createNewFile();
                        long fileLength = 0;
                        try (BufferedInputStream inputStream = new BufferedInputStream(dfResponse.getEntity().getContent(), 8192);
                             RandomAccessFile out = new RandomAccessFile(tar, "rws")) {
                            out.seek(skip);
                            byte[] buff = new byte[8192];
                            int len;
                            while (0 < (len = inputStream.read(buff))) {
                                out.write(buff, 0, len);
                                fileLength = out.length();
                            }
                        } catch (Exception e) {
                            log.error("写入文件数据时发生异常！ filePath=" + tar.getName(), e);
                            // 重置请求参数 跳过已经下载的部分
                            skip = fileLength;
                            expectedValue = 1;
                        }
                        continue;
                    }
                    if (dfResponse.getStatus() == 4) {
                        // 文件下载完毕 校验文件完整性
                        boolean right;
                        File tar = new File(filePath, lastFileName);
                        try (RandomAccessFile inputStream = new RandomAccessFile(tar, "r")) {
                            right = checkMD5(inputStream, dfResponse.getFileNane());
                        }
                        if (right) {
                            log.info("文件 %s 下载成功.", tar.getName());
                            // 文件内容校验正确
                            map.put(KEY_FILE_STATE, VAL_FILE_STATE_DOWNLOAD);
                            // 通知服务器清除该文件
                            skip = 0;
                            expectedValue = 0;
                        } else {
                            // 文件内容校验错误
                            log.error("文件下载出错！");
                            // 删除文件并重新下载
                            tar.delete();
                            log.info("错误文件已删除");
                            log.error("重新下载文件!");
                            skip = 0;
                            expectedValue = 1;
                        }
                        continue;
                    }
                } catch (HttpHostConnectException e) {
                    log.error("服务器链接失败！", e);
                    sleep(10000);
                } catch (Exception e) {
                    log.error("捕获异常！", e);
                } finally {
                    // 持久化
                    if (map.modify() < 10) {
                        map.flushWriteBuff();
                    } else {
                        map.rewriteDbFile();
                    }
                }
            }
        } catch (Exception e) {
            log.error("客户端启动失败！", e);
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
        // 设置请求和传输超时
        configBuilder.setSocketTimeout(30000).setConnectionRequestTimeout(30000);
        request.setConfig(configBuilder.build());
        return request;
    }

    private static void init() {
        printProcessInfo();
        ConfFactory.load(CONF_FILE_PATH);
        LoggerFactory.setLogPath(ConfFactory.get("logPath"));
        LoggerFactory.setLogFileName(ConfFactory.get("logFileName"));
        LoggerFactory.roll();
    }

    private static void printProcessInfo() {
        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
        String name = runtime.getName();
        System.out.println("当前进程： " + name);
        log.info("当前进程： " + name);

    }

    private static class DFResponse {
        // 0-数据一致态; 1-文件新增态，存在新的文件，fileName 存放着新文件名; 2-文件同步态，content保存着当前文件的新内容; 3-文件已遗失; 4-文件已结束; 5-keyId无效
        private int status;
        private String fileNane;
        private int version;

        private HttpEntity entity;

        private boolean complete;

        int getStatus() {
            return status;
        }

        void setStatus(int status) {
            this.status = status;
        }

        String getFileNane() {
            return fileNane;
        }

        void setFileNane(String fileNane) {
            this.fileNane = fileNane;
        }

        int getVersion() {
            return version;
        }

        void setVersion(int version) {
            this.version = version;
        }

        boolean isComplete() {
            return complete;
        }

        void setComplete(boolean complete) {
            this.complete = complete;
        }

        public HttpEntity getEntity() {
            return entity;
        }

        public void setEntity(HttpEntity entity) {
            this.entity = entity;
        }
    }

    private static void sleep(long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
        }
    }

    private static boolean checkMD5(RandomAccessFile target, String md5) throws IOException, NoSuchAlgorithmException {
        String tarMD5 = MD5Util.md5HashCode32(target);
        return md5.equals(tarMD5);
    }

    private static void download(CloseableHttpClient client, DFResponse dfResponse, File tar) {
        try {
            if (!tar.exists()) tar.createNewFile();
            CloseableHttpResponse response = client.execute(createPostReq(dfResponse.getVersion(), 0, 1));
            try (InputStream inputStream = response.getEntity().getContent();
                 OutputStream outputStream = new FileOutputStream(tar)) {
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

}
