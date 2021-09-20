package com.bolo.downloader.respool.nio.http.server.invoke;

import io.netty.handler.codec.http.FullHttpResponse;

/**
 * Mapper方法执行结果解释器
 */
public interface ResultInterpreter {
    FullHttpResponse interpret(Object result);
}
