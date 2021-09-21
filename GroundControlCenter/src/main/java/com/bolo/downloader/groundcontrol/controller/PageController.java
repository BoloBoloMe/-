package com.bolo.downloader.groundcontrol.controller;

import com.bolo.downloader.respool.nio.http.server.annotate.Controller;
import com.bolo.downloader.respool.nio.http.server.annotate.RequestMapping;
import com.bolo.downloader.respool.nio.http.server.annotate.RequestMethod;
import com.bolo.downloader.respool.nio.http.server.invoke.ResponseEntity;
import com.bolo.downloader.respool.nio.utils.PageUtil;
import io.netty.handler.codec.http.HttpResponseStatus;

import java.util.Collections;
import java.util.Optional;

@Controller
public class PageController {

    private static final byte[] emptyContent = {};

    @RequestMapping(path = {"index", "/"}, method = RequestMethod.GET)
    public ResponseEntity<byte[]> index() {
        return new ResponseEntity<>(HttpResponseStatus.OK,
                Optional.ofNullable(PageUtil.findPage("/page/index.html", Collections.emptyMap())).map(PageUtil.Page::getContent).orElse(emptyContent));
    }
}
