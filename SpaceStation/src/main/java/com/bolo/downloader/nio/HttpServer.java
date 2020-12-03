package com.bolo.downloader.nio;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;

import javax.net.ssl.SSLException;
import java.security.cert.CertificateException;

public class HttpServer {
    private final boolean SSL = System.getProperty("ssl") != null;
    private final int port;

    public HttpServer(int port) {
        this.port = port;
    }

    /**
     * 监听 http/https 请求
     */
    public void start() throws CertificateException, InterruptedException, SSLException {
        // Configure SSL.
        final SslContext sslCtx;
        if (SSL) {
            SelfSignedCertificate ssc = new SelfSignedCertificate();
            sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();
        } else {
            sslCtx = null;
        }

        // Configure the server.
        EventLoopGroup bothGroup = new NioEventLoopGroup(1);
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bothGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new HttpServerInitializer(sslCtx));
        Channel ch = bootstrap.bind(port).sync().channel();
    }
}
