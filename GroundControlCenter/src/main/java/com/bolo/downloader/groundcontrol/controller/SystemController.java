package com.bolo.downloader.groundcontrol.controller;

import com.bolo.downloader.groundcontrol.ClientBootstrap;
import com.bolo.downloader.respool.nio.http.controller.annotate.Controller;
import com.bolo.downloader.respool.nio.http.controller.annotate.RequestMapping;
import com.bolo.downloader.respool.nio.http.controller.invoke.impl.ResponseEntity;
import io.netty.handler.codec.http.HttpResponseStatus;

@Controller
@RequestMapping("system")
public class SystemController {

    @RequestMapping("shutdown")
    public ResponseEntity<String> shutdown() {
        ClientBootstrap.shutdownGracefully();
        return new ResponseEntity<>(HttpResponseStatus.OK, "服务将在5秒后结束");
    }
}
