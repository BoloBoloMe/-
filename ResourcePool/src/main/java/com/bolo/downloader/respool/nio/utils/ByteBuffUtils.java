package com.bolo.downloader.respool.nio.utils;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

public class ByteBuffUtils {
    private static final PooledByteBufAllocator allocator = PooledByteBufAllocator.DEFAULT;

    public static ByteBuf copy(byte[] data) {
        ByteBuf buf = allocator.buffer(data.length);
        buf.writeBytes(data);
        return buf;
    }

    public static ByteBuf copy(byte[] data, int len) {
        ByteBuf buf = allocator.buffer(len);
        buf.writeBytes(data, 0, len);
        return buf;
    }

    public static ByteBuf copy(String data, Charset charset) {
        byte[] db = data.getBytes(charset);
        ByteBuf buf = allocator.buffer(db.length);
        buf.writeBytes(db);
        return buf;
    }

    public static ByteBuf copy(InputStream in, int len) throws IOException {
        ByteBuf buf = allocator.buffer(len);
        buf.writeBytes(in, len);
        return buf;
    }


    public static ByteBuf bigBuff() {
        return allocator.buffer(71808);
    }

    public static ByteBuf empty() {
        return Unpooled.EMPTY_BUFFER;
    }
}
