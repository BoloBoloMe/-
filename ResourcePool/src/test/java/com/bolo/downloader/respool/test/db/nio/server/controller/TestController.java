package com.bolo.downloader.respool.test.db.nio.server.controller;

import com.bolo.downloader.respool.nio.http.server.annotate.Controller;
import com.bolo.downloader.respool.nio.http.server.annotate.RequestMapping;
import com.bolo.downloader.respool.nio.http.server.annotate.RequestMethod;
import com.bolo.downloader.respool.nio.http.server.invoke.ResponseEntity;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Controller
public class TestController {

    @RequestMapping(path = "/echo",
            method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.DELETE, RequestMethod.PATCH,
                    RequestMethod.HEAD, RequestMethod.PATCH, RequestMethod.OPTIONS, RequestMethod.PUT})
    public ResponseEntity<String> echo(MyList message, FullHttpRequest request, ChannelHandlerContext context) {
        System.out.println("echo receive:" + message);
        System.out.println("echo response:" + message);
        return new ResponseEntity<>(HttpResponseStatus.OK, message.toString());
    }

    public static class MyList extends ArrayList<String> {

    }
}
