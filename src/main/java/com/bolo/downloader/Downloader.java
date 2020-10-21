package com.bolo.downloader;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 下载器实现
 */
@Slf4j
@Service
public class Downloader {
    final private TaskList taskList = new TaskList();
    final private ReentrantLock lock = new ReentrantLock();

    @Value("${video-path}")
    private String videoPath;

    @Value("${youtube-dl-path}")
    private String youtubeDLPath;

    /**
     * 新增任务
     *
     * @return 0:已在列表中，1:添加成功
     */
    public int addTask(String url) {
        boolean forecast = taskList.hasNextPending();
        int result = taskList.add(url) ? 1 : 0;
        // 如果是列表第一个待处理任务，将自动触发任务处理线程
        if (!forecast && result == 1) {
            startTask();
        }
        return result;
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
     * 启动任务处理
     *
     * @return 0:其他线程正在处理任务,1:启动成功,2:启动异常
     */
    public int startTask() {
        if (!taskList.hasNextPending()) {
            return 1;
        }
        if (lock.isLocked()) {
            return 0;
        }
        try {
            new Thread(() -> {
                log.info("开启新线程处理任务列表:{}", Thread.currentThread().getName());
                try {
                    if (!lock.tryLock()) {
                        log.info("未能获得任务的处理权限,可能是其他线程正在处理任务，结束线程");
                        return;
                    }
                    log.info("已获得任务处理权限！");
                    while (taskList.hasNextPending()) {
                        String url = taskList.lockNextPending();
                        taskList.closure(url, Terminal.execYoutubeDL(url, youtubeDLPath));
                    }
                    log.info("本次待处理的任务已处理完毕，结束线程");
                } catch (Exception e) {
                    log.error("任务处理异常，线程中断！异常信息：" + e.getMessage(), e);
                } finally {
                    if (lock.isHeldByCurrentThread()) {
                        lock.unlock();
                    }
                }
            }).start();
        } catch (Exception e) {
            log.error("任务启动异常：" + e.getMessage(), e);
            return 2;
        }
        return 1;
    }

    /**
     * 返回视频存放路径下的文件列表
     */
    List<String> listVideo() {
        String[] vidoList = new File(new File("").getAbsolutePath()).list((dir, name) -> Pattern.matches(".+(\\.mp4|\\.webm|\\.wmv|\\.avi|\\.dat|\\.asf|\\.mpeg|\\.mpg|\\.rm|\\.rmvb|\\.ram|\\.flv|\\.3gp|\\.mov|\\.divx|\\.dv|\\.vob|\\.mkv|\\.qt|\\.cpk|\\.fli|\\.flc|\\.f4v|\\.m4v|\\.mod|\\.m2t|\\.swf|\\.mts|\\.m2ts|\\.3g2|\\.mpe|\\.ts|\\.div|\\.lavf|\\.dirac){1}", name.toLowerCase()));
        return vidoList == null ? new ArrayList<>() : Stream.of(vidoList).collect(Collectors.toCollection(ArrayList::new));
    }
}
