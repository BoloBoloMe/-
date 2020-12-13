package com.bolo.downloader.station;

import com.bolo.downloader.sync.Synchronizer;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 下载器实现
 */
public class Downloader {
    final private TaskList taskList = new TaskList();
    final static private ExecutorService taskRunner = Executors.newFixedThreadPool(5);
    private String videoPath;
    private String youtubeDLPath;

    private Synchronizer synchronizer = new Synchronizer();

    public Downloader(String videoPath, String youtubeDLPath) {
        this.videoPath = videoPath;
        this.youtubeDLPath = youtubeDLPath;
    }

    /**
     * 新增任务
     *
     * @return 0:已在列表中，1:添加成功
     */
    public int addTask(String url) {
        return taskList.add(url) && submitTask(url) ? 1 : 0;
    }

    public Map<String, String> listTasks() {
        return taskList.list();
    }

    /**
     * 清空任务列表
     */
    public void clearTasks() {
        taskList.clear();
    }


    /**
     * 提交任务
     */
    private boolean submitTask(String url) {
        taskRunner.submit(() -> {
            taskList.lockNextPending(url);
            boolean result = Terminal.execYoutubeDL(url, youtubeDLPath);
            taskList.closure(url, result);
        });
        return true;
    }

    /**
     * 返回视频存放路径下的文件列表
     */
    public List<String> listVideo() {
        return Synchronizer.fileList();
    }

    public void shudown() {
        taskRunner.shutdown();
    }
}
