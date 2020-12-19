package com.bolo.downloader.groundcontrol.handler;

import com.bolo.downloader.groundcontrol.dict.StoneMapDict;
import com.bolo.downloader.groundcontrol.factory.ConfFactory;
import com.bolo.downloader.groundcontrol.factory.StoneMapFactory;
import com.bolo.downloader.respool.db.StoneMap;
import org.apache.http.*;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.message.BasicNameValuePair;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


abstract public class AbstractResponseHandler implements ResponseHandler<HttpRequestBase> {
    private static final Set<AbstractResponseHandler> childHandlers = new HashSet<>();

    protected AbstractResponseHandler() {
    }

    public final AbstractResponseHandler join(AbstractResponseHandler otherOne) {
        if (!childHandlers.contains(this)) childHandlers.add(this);
        childHandlers.add(otherOne);
        return this;
    }

    @Override
    final public HttpRequestBase handleResponse(HttpResponse httpResponse) {
        Response response = analyzeResponse(httpResponse);
        for (AbstractResponseHandler handler : childHandlers) {
            if (handler.interested(response.getStatus())) {
                return handler.handleResponse(response);
            }
        }
        // 没有childHandlers处理响应
        StoneMap map = StoneMapFactory.getObject();
        int lastVer = Integer.parseInt(map.get(StoneMapDict.KEY_LAST_VER));
        return post(lastVer, 1);
    }

    abstract boolean interested(int responseStatus);

    abstract HttpRequestBase handleResponse(Response response);


    public static HttpPost post(int currVer, int expectedLen) {
        HttpPost request = new HttpPost(ConfFactory.get("url"));
        request.setProtocolVersion(new ProtocolVersion("http", 1, 1));
        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("cv", Integer.toString(currVer)));
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

    static Response analyzeResponse(HttpResponse resp) {
        Response response = new Response();
        if (resp.getStatusLine().getStatusCode() / 200 != 1) {
            throw new IllegalArgumentException("服务器返回操作失败响应码：" + resp.getStatusLine().getStatusCode());
        }
        try {
            // 操作码
            response.setStatus(Integer.parseInt(resp.getLastHeader("st").getValue()));
            // 文件名
            Header fn = resp.getLastHeader("fn");
            if (fn != null && !"".equals(fn.getValue())) {
                response.setFileNane(URLDecoder.decode(fn.getValue(), "utf8"));
            }
            if (response.getStatus() == 2) {
                // Socket 输入流
                response.setEntity(resp.getEntity());
                response.setMd5(resp.getLastHeader("md").getValue());
                response.setContentLength(Long.parseLong(resp.getLastHeader("content-length").getValue()));
            } else {
                response.setVersion(Integer.parseInt(resp.getLastHeader("vs").getValue()));
            }
            return response;
        } catch (Exception e) {
            throw new IllegalArgumentException("服务器响应参数解析异常！", e);
        }
    }

    protected static class Response {
        // 0-数据一致态; 1-文件新增态，存在新的文件，fileName 存放着新文件名; 2-文件同步态，content保存着当前文件的新内容; 3-文件已遗失; 4-文件已结束; 5-keyId无效
        private int status;
        private int version;
        private String fileNane;
        private String md5;
        private long contentLength;
        private HttpEntity entity;

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

        String getMd5() {
            return md5;
        }

        void setMd5(String md5) {
            this.md5 = md5;
        }

        long getContentLength() {
            return contentLength;
        }

        void setContentLength(long contentLength) {
            this.contentLength = contentLength;
        }

        HttpEntity getEntity() {
            return entity;
        }

        void setEntity(HttpEntity entity) {
            this.entity = entity;
        }
    }
}
