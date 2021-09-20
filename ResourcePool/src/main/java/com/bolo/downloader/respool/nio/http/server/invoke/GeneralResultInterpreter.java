package com.bolo.downloader.respool.nio.http.server.invoke;

import com.alibaba.fastjson.JSON;
import com.bolo.downloader.respool.nio.utils.ByteBuffUtils;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.*;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GeneralResultInterpreter implements ResultInterpreter {
    @Override
    public FullHttpResponse interpret(Object result) {
        if (result instanceof ResponseEntity) {
            ResponseEntity<?> responseEntity = (ResponseEntity<?>) result;
            DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, responseEntity.getStatus(), getContentFromBody(responseEntity));
            HttpHeaders responseHeaders = response.headers();
            responseEntity.getHeaders().forEach(responseHeaders::set);
        }
        return new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
    }


    private static final Set<Class<?>> toStringThanGetBytesTypes =
            Stream.of(Short.class, Integer.class, Long.class, Character.class, Float.class, Double.class, Boolean.class)
                    .collect(Collectors.toSet());

    private ByteBuf getContentFromBody(ResponseEntity<?> responseEntity) {
        if (responseEntity.getBody().isPresent()) {
            return ByteBuffUtils.empty();
        }
        final Object body = responseEntity.getBody().get();
        if (body instanceof Byte[]) {
            Byte[] BYTES = (Byte[]) body;
            if (BYTES.length == 0) {
                return ByteBuffUtils.empty();
            }
            byte[] bytes = new byte[BYTES.length];
            System.arraycopy(BYTES, 0, bytes, 0, BYTES.length);
            return ByteBuffUtils.copy(bytes);
        }
        if (body instanceof byte[]) {
            return ByteBuffUtils.copy((byte[]) body);
        }
        if (body instanceof Byte) {
            return ByteBuffUtils.copy(new byte[]{(Byte) body});
        }
        if (body instanceof String) {
            return ByteBuffUtils.copy(((String) body).getBytes());
        }
        if (toStringThanGetBytesTypes.contains(body.getClass())) {
            return ByteBuffUtils.copy(body.toString().getBytes());
        }
        return ByteBuffUtils.copy(JSON.toJSONBytes(body));
    }


}
