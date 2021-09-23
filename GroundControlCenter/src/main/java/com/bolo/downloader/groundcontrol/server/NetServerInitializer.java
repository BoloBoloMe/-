package com.bolo.downloader.groundcontrol.server;

import com.bolo.downloader.respool.nio.http.controller.HttpDistributeHandler;
import com.bolo.downloader.respool.nio.http.controller.scan.impl.ClasspathScanner;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;

import java.nio.charset.Charset;


public class NetServerInitializer extends ChannelInitializer<SocketChannel> {

    private final static HttpDistributeHandler DISTRIBUTE_HANDLER = HttpDistributeHandler.newBuilder()
            .setScanner(new ClasspathScanner("com.bolo.downloader.groundcontrol.controller"))
            .setRootPath("gcc")
            .build();

    @Override
    public void initChannel(SocketChannel ch) {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast(new StringEncoder(Charset.forName("UTF-8")));
        pipeline.addLast(new IdleStateHandler(60, 60, 0));
        pipeline.addLast(new HttpServerCodec());
        pipeline.addLast(new HttpObjectAggregator(1024));
        pipeline.addLast(new ChunkedWriteHandler());
        pipeline.addLast(DISTRIBUTE_HANDLER);
        pipeline.addLast(StaticPageHandler.getInstance());
    }
}
