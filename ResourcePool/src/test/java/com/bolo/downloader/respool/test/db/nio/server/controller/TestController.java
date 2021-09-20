package com.bolo.downloader.respool.test.db.nio.server.controller;

import com.bolo.downloader.respool.nio.http.server.annotate.Controller;
import com.bolo.downloader.respool.nio.http.server.annotate.RequestMapping;
import com.bolo.downloader.respool.nio.http.server.annotate.RequestMethod;
import com.bolo.downloader.respool.nio.http.server.invoke.ResponseEntity;
import io.netty.handler.codec.http.HttpResponseStatus;

@Controller
public class TestController {

    @RequestMapping(path = "echo", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<String> echo(String message) {
        System.out.println("echo receive:" + message);
        System.out.println("echo response:" + message);
        return new ResponseEntity<>(HttpResponseStatus.OK, message);
    }
}
