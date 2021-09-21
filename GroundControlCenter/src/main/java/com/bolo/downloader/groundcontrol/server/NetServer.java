package com.bolo.downloader.groundcontrol.server;

import com.bolo.downloader.groundcontrol.factory.ConfFactory;
import com.bolo.downloader.respool.log.LoggerFactory;
import com.bolo.downloader.respool.log.MyLogger;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class NetServer {
    private EventLoopGroup bossGroup;
    private EventLoopGroup workGroup;
    private final int port;
    private MyLogger log = LoggerFactory.getLogger(NetServer.class);

    public NetServer(int port) {
        this.port = port;
    }

    /**
     * 监听 http/https 请求
     */
    public void start() {
        // Configure the server.
        int bossThreadCount = Integer.parseInt(ConfFactory.get("mediaBossThreadCount"));
        int workThreadCount = Integer.parseInt(ConfFactory.get("mediaWorkThreadCount"));
        bossGroup = new NioEventLoopGroup(bossThreadCount);
        workGroup = new NioEventLoopGroup(workThreadCount);
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new NetServerInitializer());
        bootstrap.bind(port);
        String startLog = "服务启动成功,地址：http://127.0.0.1:" + port;
        log.info(startLog);
        System.out.println(startLog);
    }

    public void shutdown() {
        bossGroup.shutdownGracefully();
    }
}
