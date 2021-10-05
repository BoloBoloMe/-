package com.bolo.downloader.groundcontrol.controller;

import com.bolo.downloader.groundcontrol.util.FileMap;
import com.bolo.downloader.respool.nio.http.controller.annotate.Controller;
import com.bolo.downloader.respool.nio.http.controller.annotate.RequestMapping;
import com.bolo.downloader.respool.nio.http.controller.invoke.impl.ResponseEntity;
import com.bolo.downloader.respool.nio.utils.FileTransferUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;

import java.io.File;
import java.util.HashMap;
import java.util.Objects;

@Controller
@RequestMapping("media")
public class MediaController {

    @RequestMapping("fl")
    public ResponseEntity<String> fileList() {
        return new ResponseEntity<>(HttpResponseStatus.OK, FileMap.fullListJson())
                .addHeader(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=UTF-8");
    }

    @RequestMapping("dl")
    public ResponseEntity<String> download(String tar, ChannelHandlerContext ctx, FullHttpRequest request) {
        if (Objects.isNull(tar) || tar.isEmpty()) {
            return new ResponseEntity<>(HttpResponseStatus.PAYMENT_REQUIRED, "缺少必要参数！请指定要播放的媒体文件");
        }
        String absolutePaths = FileMap.findByFileName(tar);
        if (Objects.isNull(absolutePaths)) {
            return new ResponseEntity<>(HttpResponseStatus.NOT_FOUND, "未找到指定的文件");
        }
        HashMap<String, Object> headers = new HashMap<>(1);
        headers.put(HttpHeaderNames.CONTENT_DISPOSITION.toString(),
                "attachment;filename=\"" + absolutePaths.substring(absolutePaths.lastIndexOf(File.pathSeparatorChar)) + "\"");
        FileTransferUtil.sendFile(ctx, request, absolutePaths, headers);
        return null;
    }

    @RequestMapping("pl")
    public ResponseEntity<String> playVideo(String tar, ChannelHandlerContext ctx, FullHttpRequest request) {
        if (tar == null || tar.isEmpty()) {
            return new ResponseEntity<>(HttpResponseStatus.PAYMENT_REQUIRED, "缺少必要参数！请指定要播放的媒体文件");
        }
        String absolutePaths = FileMap.findByFileName(tar);
        if (Objects.isNull(absolutePaths)) {
            return new ResponseEntity<>(HttpResponseStatus.NOT_FOUND, "未找到指定的文件");
        }
        HashMap<String, Object> headers = new HashMap<>(1);
        headers.put(HttpHeaderNames.CONTENT_DISPOSITION.toString(), "inline");
        FileTransferUtil.sendFile(ctx, request, absolutePaths, headers);
        return null;
    }
}
