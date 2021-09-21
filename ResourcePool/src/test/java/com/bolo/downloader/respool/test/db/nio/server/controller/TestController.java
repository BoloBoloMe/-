package com.bolo.downloader.respool.test.db.nio.server.controller;

import com.bolo.downloader.respool.nio.http.server.RequestContextHolder;
import com.bolo.downloader.respool.nio.http.server.annotate.Controller;
import com.bolo.downloader.respool.nio.http.server.annotate.RequestMapping;
import com.bolo.downloader.respool.nio.http.server.annotate.RequestMethod;
import com.bolo.downloader.respool.nio.http.server.invoke.ResponseEntity;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;

import java.util.*;

@Controller
public class TestController {

    @RequestMapping(path = "/echo",
            method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.DELETE, RequestMethod.PATCH,
                    RequestMethod.HEAD, RequestMethod.PATCH, RequestMethod.OPTIONS, RequestMethod.PUT})
    public ResponseEntity<String> echo(FullHttpRequest request, ChannelHandlerContext context) {
        MyList message = new MyList();
        RequestContextHolder.getValues("message").ifPresent(message::addAll);
        System.out.println("echo receive:" + message);
        System.out.println("echo response:" + message);
        return new ResponseEntity<>(HttpResponseStatus.OK, Objects.toString(message));
    }

    public static class MyList extends ArrayList<String> {

    }
}
