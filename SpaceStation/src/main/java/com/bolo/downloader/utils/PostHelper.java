package com.bolo.downloader.utils;

import com.bolo.downloader.factory.DownloaderFactory;
import com.bolo.downloader.station.Downloader;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;

import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * restful 操作助手
 */
public class PostHelper {
    private static Downloader downloader = DownloaderFactory.getObject();

    /**
     * @return isDone, 返回true将在方法结束后尝试关闭渠道
     */
    public static boolean doPOST(String uri, Map<String, List<String>> params, ChannelHandlerContext ctx, FullHttpRequest request) {
        if (uri.equals("/df")) {
            return FileDownloadUtils.download(uri, params, ctx, request);
        }
        if (uri.equals("/task/add")) {
            if (params.get("url") == null || params.get("url").size() <= 0) {
                ResponseHelper.sendText(ctx, HttpResponseStatus.OK, request, "请输入下载地址");
                return true;
            }
            String targetAddr;
            try {
                targetAddr = new String(Base64.getDecoder().decode(params.get("url").get(0)));
            } catch (Exception e) {
                e.printStackTrace();
                ResponseHelper.sendText(ctx, HttpResponseStatus.OK, request, "下载地址解析失败！" + e.getMessage());
                return true;
            }
            String result = downloader.addTask(targetAddr) == 1 ? "添加成功" : "任务已在列表中！";
            ResponseHelper.sendText(ctx, HttpResponseStatus.OK, request, result);
            return true;
        }
        if (uri.equals("/task/clear")) {
            downloader.clearTasks();
            ResponseHelper.sendText(ctx, HttpResponseStatus.OK, request, "列表已清空");
        }
        ResponseHelper.sendText(ctx, HttpResponseStatus.OK, request, "未知的地址");
        return true;
    }
}