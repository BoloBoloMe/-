package com.bolo.downloader;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 下载器实现
 */
@Slf4j
@Service
public class Downloader {
    final private TaskList taskList = new TaskList();
    final private ReentrantLock lock = new ReentrantLock();

    @Value("videoPath")
    private String videoPath;

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

    public Map<String, String> list() {
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
                        taskList.closure(url, callYoutubeDL(url));
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

    private boolean callYoutubeDL(String url) {
        return Terminal.execYoutubeDL(url, videoPath);
    }

}
