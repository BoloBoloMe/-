package com.bolo.downloader.groundcontrol.server;

import com.bolo.downloader.groundcontrol.factory.ConfFactory;
import com.bolo.downloader.groundcontrol.util.MainPool;
import com.bolo.downloader.respool.log.LoggerFactory;
import com.bolo.downloader.respool.log.MyLogger;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class NetServer {
    private EventLoopGroup bossGroup;
    private EventLoopGroup workGroup;
    private final int port;
    static final private MyLogger log = LoggerFactory.getLogger(NetServer.class);

    final static private Map<Integer, NetServer> SERVICE_MAP = new ConcurrentHashMap<>();

    public NetServer(int port) {
        this.port = port;
    }

    /**
     * 监听 http/https 请求
     */
    public static void start(int port) {
        NetServer server = new NetServer(port);
        // Configure the server.
        int bossThreadCount = Integer.parseInt(ConfFactory.get("mediaBossThreadCount"));
        int workThreadCount = Integer.parseInt(ConfFactory.get("mediaWorkThreadCount"));
        final AtomicInteger workCount = new AtomicInteger();
        server.bossGroup = new NioEventLoopGroup(bossThreadCount, MainPool.executor());
        server.workGroup = new NioEventLoopGroup(workThreadCount, runnable -> {
            Thread thread = new Thread(runnable, "netty-work-thread-" + workCount.incrementAndGet());
            thread.setDaemon(false);
            return thread;
        });
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(server.bossGroup, server.workGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new NetServerInitializer());
        bootstrap.bind(port);
        SERVICE_MAP.put(port, server);
        String startLog = "服务启动成功,地址：http://127.0.0.1:" + port + "/";
        log.info(startLog);
        System.out.println(startLog);
    }

    public void shutdown() {
        if (bossGroup.isShutdown()) {
            bossGroup.shutdownGracefully();
        }
        if (workGroup.isShutdown()) {
            workGroup.shutdownGracefully();
        }
    }

    public int getPort() {
        return port;
    }
}
