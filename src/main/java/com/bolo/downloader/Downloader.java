package com.bolo.downloader;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.locks.ReentrantLock;

/**
 * 下载器实现
 */
@Service
@Slf4j
public class Downloader {

    private TaskList taskList = new TaskList();
    private ReentrantLock lock = new ReentrantLock();

    /**
     * 新增任务
     *
     * @return 0:已在列表中，1:添加成功
     */
    public int addTask(String url) {
        return taskList.add(url) ? 1 : 0;
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
                try {
                    if (!lock.tryLock()) {
                        return;
                    }
                    while (true) {
                        TaskList.Task startPoint = null;
                        TaskList.Task task = taskList.next();
                        if (task.getStatus() == 0) {
                            if (startPoint == null) {
                                startPoint = task;
                            }
                            task.setStatus(1);
                            task.setStatus(callYoutubeDL(task.getUrl()) ? 2 : 3);
                        } else {
                            if (task == startPoint) {
                                break;
                            }
                        }
                    }
                } catch (Exception e) {
                    log.error("任务处理异常：" + e.getMessage(), e);
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
        return true;
    }

}
