package com.bolo.downloader.groundcontrol.controller;

import com.bolo.downloader.groundcontrol.util.FileMap;
import com.bolo.downloader.respool.nio.http.controller.annotate.Controller;
import com.bolo.downloader.respool.nio.http.controller.annotate.RequestMapping;
import com.bolo.downloader.respool.nio.http.controller.annotate.RequestMethod;
import com.bolo.downloader.respool.nio.http.controller.invoke.impl.ResponseEntity;
import com.bolo.downloader.respool.nio.utils.FileTransferUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;

import java.util.HashMap;
import java.util.Objects;

/**
 * 媒体播放器 Controller
 */
@Controller
public class DownloadController {

    @RequestMapping("fl")
    public ResponseEntity<String> fileList() {
        return new ResponseEntity<>(HttpResponseStatus.OK, FileMap.fullListJson())
                .addHeader(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=UTF-8");
    }
    
}
