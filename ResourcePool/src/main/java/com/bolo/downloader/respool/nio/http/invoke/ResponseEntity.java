package com.bolo.downloader.respool.nio.http.invoke;

import com.sun.istack.internal.NotNull;
import io.netty.handler.codec.http.HttpResponseStatus;

public class ResponseEntity {
    private HttpResponseStatus status;
    private byte[] data;
    private final static byte[] emptyData = {};


    public ResponseEntity(@NotNull HttpResponseStatus status) {
        this(status, (byte[]) null);
    }

    public ResponseEntity(@NotNull HttpResponseStatus status, String data) {
        this.status = status;
        this.data = data.getBytes();
    }

    public ResponseEntity(@NotNull HttpResponseStatus status, byte[] data) {
        this.status = status;
        this.data = data == null ? emptyData : data;
    }

    @NotNull
    public HttpResponseStatus getStatus() {
        return status;
    }

    @NotNull
    public byte[] getData() {
        return data;
    }
}
