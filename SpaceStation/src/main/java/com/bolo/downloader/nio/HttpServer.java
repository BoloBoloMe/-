package com.bolo.downloader.nio;

import com.bolo.downloader.factory.ConfFactory;
import com.bolo.downloader.respool.log.LoggerFactory;
import com.bolo.downloader.respool.log.MyLogger;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLException;
import java.io.*;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

public class HttpServer {
    private final boolean ssl;
    private final int port;
    private MyLogger log = LoggerFactory.getLogger(HttpServer.class);
    private EventLoopGroup bothGroup = new NioEventLoopGroup(1);

    public HttpServer(int port, boolean ssl) {
        this.port = port;
        this.ssl = ssl;
    }

    /**
     * 监听 http/https 请求
     */
    public void start() throws Exception {
        // Configure SSL.
        final SslContext sslCtx;
        if (ssl) {
            String keystorePath = ConfFactory.get("keystore");
            char[] password = ConfFactory.get("keystore.password").toCharArray();
            KeyStore keyStore = KeyStore.getInstance("JKS");
            try (InputStream in = new FileInputStream(keystorePath)) {
                keyStore.load(in, password);
            } catch (Exception e) {
                log.error("SSL密钥加载失败！");
                throw e;
            }
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            //初始化密钥管理器
            kmf.init(keyStore, password);
            sslCtx = SslContextBuilder.forServer(kmf).build();
        } else {
            sslCtx = null;
        }

        // Configure the server.
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bothGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new HttpServerInitializer(sslCtx));
        bootstrap.bind(port);
        log.info("服务启动成功,地址：" + (ssl ? "https://" : "http://") + "127.0.0.1:" + port);
    }

    public void shutdown() {
        bothGroup.shutdownGracefully();
    }
}
