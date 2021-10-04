package com.bolo.downloader.groundcontrol.controller;

import com.bolo.downloader.respool.nio.http.controller.annotate.Controller;
import com.bolo.downloader.respool.nio.http.controller.annotate.RequestMapping;
import com.bolo.downloader.respool.nio.http.controller.invoke.impl.ResponseEntity;
import com.bolo.downloader.respool.nio.utils.FileTransferUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;

import java.util.HashMap;
import java.util.Objects;

@Controller
public class MediaController {
    @RequestMapping("dl")
    public ResponseEntity<String> download(String tar, ChannelHandlerContext ctx, FullHttpRequest request) {
        if (Objects.isNull(tar) || tar.isEmpty()) {
            return new ResponseEntity<>(HttpResponseStatus.PAYMENT_REQUIRED, "缺少必要参数！请指定要播放的媒体文件");
        }
//        HashMap<String, Objects> headers = new HashMap<>(1);
//        headers.put(HttpHeaderNames.CONTENT_DISPOSITION, "attachment;filename=\"" + file.getName() + "\"");
//        FileTransferUtil.sendFile(ctx, request, , headers);
        return null;
    }
}
