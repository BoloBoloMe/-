package com.bolo.downloader.nio;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;

import java.util.List;
import java.util.Map;

public class ReqRecord {
    private HttpMethod method;
    private String uri;
    private Map<String, List<String>> params;
    private ChannelHandlerContext ctx;
    private FullHttpRequest request;

    // 链表相关字段
    private ReqRecord next = null;
    private ReqRecord prev = null;
    private boolean done;


    public ReqRecord(HttpMethod method, String uri, Map<String, List<String>> params, ChannelHandlerContext ctx, FullHttpRequest request) {
        this.method = method;
        this.uri = uri;
        this.params = params;
        this.ctx = ctx;
        this.request = request;
    }

    public HttpMethod getMethod() {
        return method;
    }

    public String getUri() {
        return uri;
    }

    public Map<String, List<String>> getParams() {
        return params;
    }

    public ChannelHandlerContext getCtx() {
        return ctx;
    }

    public FullHttpRequest getRequest() {
        return request;
    }

    /**
     * 加入环形链表
     *
     * @param prev
     * @param next
     */
    public void putLinked(ReqRecord prev, ReqRecord next) {
        this.prev = prev;
        this.next = next;
        if (prev == next) {
            // this is first node
            prev.next = this;
            next.prev = this;
        }
    }

    /**
     * 移出环形链表
     */
    public void delLinked() {
        prev.next = this.next;
        next.prev = this.prev;
    }

    public ReqRecord getNext() {
        return next;
    }

    public ReqRecord getPrev() {
        return prev;
    }

    public boolean isDone() {
        return done;
    }

    public void setDone(boolean done) {
        this.done = done;
    }
}
