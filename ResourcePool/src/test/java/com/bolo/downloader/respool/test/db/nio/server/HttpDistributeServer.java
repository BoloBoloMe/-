package com.bolo.downloader.respool.test.db.nio.server;

import com.bolo.downloader.respool.nio.http.server.HttpDistributeHandler;
import com.bolo.downloader.respool.nio.http.server.scan.ClasspathScanner;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.stream.ChunkedWriteHandler;

import java.nio.charset.Charset;

public class HttpDistributeServer {
    public static void main(String[] args) {
        // Configure the server.
        NioEventLoopGroup bossGroup = new NioEventLoopGroup(1);
        NioEventLoopGroup workGroup = new NioEventLoopGroup(1);
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ServerInitializer());
        final int port = 8080;
        bootstrap.bind(port);
        String startLog = "服务启动成功,地址：http://127.0.0.1:" + port;
        System.out.println(startLog);
    }


    private static class ServerInitializer extends ChannelInitializer<SocketChannel> {
        private static final HttpDistributeHandler distributeHandler =
                HttpDistributeHandler.newBuilder().setScanner(new ClasspathScanner("com.bolo.downloader.respool.test.db.nio.server.controller")).build();

        @Override
        public void initChannel(SocketChannel ch) {
            ChannelPipeline pipeline = ch.pipeline();
            pipeline.addLast(new StringEncoder(Charset.forName("UTF-8")));
            pipeline.addLast(new HttpServerCodec());
            pipeline.addLast(new HttpObjectAggregator(1024));
            pipeline.addLast(new ChunkedWriteHandler());
            pipeline.addLast(distributeHandler);
        }
    }
}
