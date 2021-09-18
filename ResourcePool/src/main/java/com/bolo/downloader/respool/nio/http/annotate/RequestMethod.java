package com.bolo.downloader.respool.nio.http.annotate;

import io.netty.handler.codec.http.HttpMethod;

public enum RequestMethod {
    GET(HttpMethod.GET),
    HEAD(HttpMethod.HEAD),
    POST(HttpMethod.POST),
    PUT(HttpMethod.PUT),
    PATCH(HttpMethod.PATCH),
    DELETE(HttpMethod.DELETE),
    OPTIONS(HttpMethod.OPTIONS),
    TRACE(HttpMethod.TRACE);

    private HttpMethod httpMethod;

    RequestMethod(HttpMethod httpMethod) {
        this.httpMethod = httpMethod;
    }

    public HttpMethod getHttpMethod() {
        return httpMethod;
    }
}
