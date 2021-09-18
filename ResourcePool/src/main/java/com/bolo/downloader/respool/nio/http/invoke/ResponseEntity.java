package com.bolo.downloader.respool.nio.http.invoke;


import io.netty.handler.codec.http.HttpResponseStatus;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class ResponseEntity<D> {
    /**
     * 响应状态
     */
    private final HttpResponseStatus status;

    /**
     * 响应头
     */
    private final Map<CharSequence, Object> headers;

    /**
     * 响应数据
     */
    private final D body;


    public ResponseEntity(HttpResponseStatus status) {
        this(status, null);
    }

    public ResponseEntity(HttpResponseStatus status, D data) {
        this.status = status;
        this.body = data;
        this.headers = new LinkedHashMap<>();
    }


    public HttpResponseStatus getStatus() {
        return status;
    }

    public Map<CharSequence, Object> getHeaders() {
        return headers;
    }

    public Optional<D> getBody() {
        return Optional.ofNullable(body);
    }
}
