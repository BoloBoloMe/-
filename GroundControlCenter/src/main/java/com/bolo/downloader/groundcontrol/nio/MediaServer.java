package com.bolo.downloader.groundcontrol.nio;

import com.bolo.downloader.groundcontrol.util.FileMap;
import com.bolo.downloader.groundcontrol.util.HttpPlayer;
import com.bolo.downloader.respool.log.LoggerFactory;
import com.bolo.downloader.respool.log.MyLogger;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class MediaServer {
    private EventLoopGroup bothGroup;
    private final int port;
    private MyLogger log = LoggerFactory.getLogger(MediaServer.class);

    public MediaServer(int port) {
        this.port = port;
    }

    /**
     * 监听 http/https 请求
     */
    public void start() {
        // Configure the server.
        bothGroup = new NioEventLoopGroup(1);
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bothGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new MediaServerInitializer());
        bootstrap.bind(port);
        FileMap.startFlushTask();
        log.info("服务启动成功,地址：http://127.0.0.1:" + port);
    }

    public void shutdown() {
        bothGroup.shutdownGracefully();
    }
}
