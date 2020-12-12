package com.bolo.downloader.groundcontrol;


import com.bolo.downloader.groundcontrol.factory.ConfFactory;
import com.bolo.downloader.groundcontrol.factory.StoneMapFactory;
import com.bolo.downloader.respool.coder.MD5Util;
import com.bolo.downloader.respool.coder.RSAUtils;
import com.bolo.downloader.respool.db.StoneMap;
import com.bolo.downloader.respool.log.LoggerFactory;
import com.bolo.downloader.respool.log.MyLogger;
import org.apache.http.Header;
import org.apache.http.NameValuePair;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.SocketTimeoutException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class Cantor {
    public static final String CONF_FILE_PATH = "conf/GroundControlCenter.conf";
    static private final MyLogger log = LoggerFactory.getLogger(Cantor.class);
    static private final int INIT_EXPECTED_LEN = 2048;
    static private final String KEY_LAST_VER = "lastVer";
    static private final String KEY_IS_DONE = "isDone";
    static private final ResponseHandler<DFResponse> RESPONSE_COVER = resp -> {
        DFResponse dfResponse = new DFResponse();
        if (resp.getStatusLine().getStatusCode() / 100 != 2) {
            throw new RuntimeException("服务器返回操作失败响应码：" + resp.getStatusLine().getStatusCode());
        }
        try (InputStream in = resp.getEntity().getContent()) {
            dfResponse.setStatus(Integer.parseInt(resp.getLastHeader("st").getValue()));
            dfResponse.setSkip(Long.parseLong(resp.getLastHeader("sp").getValue()));
            dfResponse.setVersion(Integer.parseInt(resp.getLastHeader("vs").getValue()));
            dfResponse.setKeyId(resp.getLastHeader("ki") != null ? resp.getLastHeader("ki").getValue() : null);
            if (dfResponse.getStatus() == 2) {
                int conLen = Integer.parseInt(resp.getLastHeader("content-length").getValue());
                byte[] content = new byte[conLen];
                int realLen = 0;
                while (realLen < conLen) {
                    realLen += in.read(content, realLen, conLen);
                }
                if (realLen != conLen) {
                    throw new RuntimeException("响应内容不完整，content-length=" + conLen + ", 实际获取长度=" + realLen);
                }
                dfResponse.setContent(content);
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
        } catch (Exception e) {
            dfResponse.setComplete(false);
            log.error("服务器响应解析失败！", e);
        }
        return dfResponse;
    };


    public static void main(String[] args) {
        init();
        printProcessInfo();
        boolean encrypt = Integer.parseInt(ConfFactory.get("rsa")) > 0;
        // 客户端状态，当状态发生变化，需要打印日志; 单次请求的文件内容长度; 每10次请求读超时数;
        int clientStatus = -1, expectedLen = INIT_EXPECTED_LEN, readTimeoutCount = 0;
        // stoneMap组成：lastVerKey->lastVer; lastVer->lastFileName
        final StoneMap map = StoneMapFactory.getObject();
        int lastVer = 0;
        long skip = 0;
        String lastFileName = null;
        if (null != map.get(KEY_LAST_VER) && (lastVer = Integer.parseInt(map.get(KEY_LAST_VER))) > 0) {
            lastFileName = map.get(map.get(KEY_LAST_VER));
        }
        try (CloseableHttpClient client = HttpClientFactory.http()) {
            File tar = null;
            if (lastFileName != null) {
                if (KEY_IS_DONE.equals(map.get(KEY_IS_DONE))) {
                    expectedLen = 0;
                    skip = 0;
                } else {
                    tar = new File(ConfFactory.get("filePath"), lastFileName);
                    if (!tar.exists()) {
                        tar.createNewFile();
                    }
                    skip = tar.length();
                }
            }
            // synchronous loop
            RSAUtils.KeyHolder key = null;
            String keyId = null;
            if (encrypt) {
                key = RSAUtils.genKey();
            }
            log.info("客户端启动成功.");
            log.info("当前版本号: %d", lastVer);
            log.info("服务器地址: %s", ConfFactory.get("url"));
            log.info("RSA加密是否开启: %s", encrypt ? "是" : "否");
            for (int time = 1; ; time++) {
                try {
                    DFResponse dfResponse = client.execute(createPostReq(lastVer, skip, expectedLen, encrypt ? key.publicKeyString() : "", keyId), RESPONSE_COVER);
                    if (!dfResponse.isComplete()) {
                        log.error("服务器响应不可用，重新请求");
                        sleep(1000);
                        continue;
                    }
                    if (dfResponse.getKeyId() != null) {
                        keyId = dfResponse.getKeyId();
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
                        // 有新的文件，创建文件
                        if (encrypt) {
                            byte[] encode = Base64.getDecoder().decode(dfResponse.getFileNane());
                            dfResponse.setFileNane(new String(RSAUtils.decode(encode, encode.length, key.getPrivateKey()), Charset.forName("utf8")));
                        }
                        if (clientStatus != 1) {
                            log.info("status: create new file：%s", dfResponse.getFileNane());
                            clientStatus = 1;
                        } else {
                            log.error("服务器连续返回新增文件响应！产生空文件：" + tar.getName());
                        }
                        map.put(KEY_LAST_VER, Integer.toString(lastVer = dfResponse.getVersion()));
                        map.put(map.get(KEY_LAST_VER), dfResponse.getFileNane());
                        tar = new File(ConfFactory.get("filePath"), dfResponse.getFileNane());
                        if (tar.exists()) tar.delete();
                        tar.createNewFile();
                        skip = 0;
                        // 重置预期文件内容长度
                        expectedLen = INIT_EXPECTED_LEN;
                        continue;
                    }
                    if (dfResponse.getStatus() == 2) {
                        // 当前文件有数据需要继续写入
                        if (clientStatus != 2) {
                            log.info("status: write file: %s", tar.getName());
                            clientStatus = 2;
                        }
                        log.info("当前流量: %d byte.", dfResponse.getContent().length);
                        if (skip != dfResponse.getSkip()) {
                            log.error("响应的数据起始位置与请求的不一致！request skip=" + skip + ",response skip=" + dfResponse.getSkip());
                            continue;
                        }
                        try (RandomAccessFile accessFile = new RandomAccessFile(tar, "rws")) {
                            accessFile.seek(dfResponse.skip);
                            byte[] writeBuf = encrypt ? RSAUtils.decode(dfResponse.getContent(), dfResponse.getContent().length, key.getPrivateKey()) : dfResponse.getContent();
                            accessFile.write(writeBuf);
                            skip = accessFile.length();
                        } catch (Exception e) {
                            log.error("文件写入失败！fileName：" + tar.getName(), e);
                            keyId = null;
                        }
                        continue;
                    }
                    if (dfResponse.getStatus() == 4) {
                        // 文件下载完毕 校验文件完整性
                        boolean right;
                        try (RandomAccessFile accessFile = new RandomAccessFile(tar, "r")) {
                            right = checkMD5(accessFile, dfResponse.getFileNane());
                        }
                        if (right) {
                            log.info("文件 %s 下载成功.", tar.getName());
                            // 文件内容校验正确,通知服务器清除该文件
                            skip = 0;
                            expectedLen = 0;
                            map.put(KEY_IS_DONE, KEY_IS_DONE);
                        } else {
                            // 文件内容校验错误,重新下载最后一部分数据
                            log.error("文件下载出错！");
                            // 删除文件并重新下载
                            tar.delete();
                            log.info("错误文件已删除");
                            sleep(15000);
                            skip = 0;
                            expectedLen = INIT_EXPECTED_LEN;
                            log.error("重新下载文件!");
                        }
                        continue;
                    }
                    if (dfResponse.getStatus() == 5) {
                        // key id 无效
                        keyId = null;
                        continue;
                    }
                } catch (HttpHostConnectException e) {
                    log.error("服务器链接失败！", e);
                    sleep(10000);
                } catch (SocketTimeoutException e) {
                    log.error("等待响应超时！", e);
                    readTimeoutCount++;
                } catch (Exception e) {
                    log.error("捕获异常！", e);
                } finally {
                    // 持久化
                    if (map.modify() < 10) {
                        map.flushWriteBuff();
                    } else {
                        map.rewriteDbFile();
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
            }
        } catch (Exception e) {
            log.error("客户端启动失败！", e);
        }
    }


    private static HttpPost createPostReq(int currVer, long skip, int expectedLen, String publicKey, String keyId) {
        HttpPost request = new HttpPost(ConfFactory.get("url"));
        request.setProtocolVersion(new ProtocolVersion("http", 1, 1));
        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("cv", Integer.toString(currVer)));
        params.add(new BasicNameValuePair("sp", Long.toString(skip)));
        params.add(new BasicNameValuePair("el", Integer.toString(expectedLen)));
        if (keyId != null) {
            params.add(new BasicNameValuePair("ki", keyId));
        } else if (null != publicKey) {
            params.add(new BasicNameValuePair("pc", publicKey));
        }
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
        private long skip;
        private String keyId;

        private byte[] content;

        private boolean complete;

        void setContent(byte[] content) {
            this.content = content;
        }

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

        long getSkip() {
            return skip;
        }

        void setSkip(long skip) {
            this.skip = skip;
        }

        byte[] getContent() {
            return content;
        }

        boolean isComplete() {
            return complete;
        }

        void setComplete(boolean complete) {
            this.complete = complete;
        }

        public String getKeyId() {
            return keyId;
        }

        public void setKeyId(String keyId) {
            this.keyId = keyId;
        }
    }

    private static void sleep(long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
        }
    }

    private static boolean checkMD5(File target, String md5) throws IOException, NoSuchAlgorithmException {
        String tarMD5 = MD5Util.md5HashCode32(new RandomAccessFile(target, "r"));
        return md5.equals(tarMD5);
    }

    private static boolean checkMD5(RandomAccessFile target, String md5) throws IOException, NoSuchAlgorithmException {
        String tarMD5 = MD5Util.md5HashCode32(target);
        return md5.equals(tarMD5);
    }

    private static boolean checkMD5(byte[] con, String md5) throws IOException, NoSuchAlgorithmException {
        String tarMD5 = MD5Util.md5HashCode32(con);
        return md5.equals(tarMD5);
    }

}
