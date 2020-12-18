package com.bolo.downloader.nio;

import com.bolo.downloader.nio.handler.HttpServerHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;

import java.nio.charset.Charset;

public class HttpServerInitializer extends ChannelInitializer<SocketChannel> {


    @Override
    public void initChannel(SocketChannel ch) {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast(new StringEncoder(Charset.forName("UTF-8")));
        pipeline.addLast(new IdleStateHandler(60, 60, 0));
        pipeline.addLast(new HttpServerCodec());
        pipeline.addLast(new HttpObjectAggregator(1024));
        pipeline.addLast(new ChunkedWriteHandler());
        pipeline.addLast(new HttpServerHandler());
    }
}
