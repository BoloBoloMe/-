package com.bolo.downloader;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
    private ExecutorService threadPool = Executors.newFixedThreadPool(1);

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
        int result = taskList.add(url) ? 1 : 0;
        submitTask();
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
    private int submitTask() {
        try {
            threadPool.submit(() -> {
                log.info("任务处理线程：{}", Thread.currentThread().getName());
                String url = taskList.lockNextPending();
                if (!"".equals(url)) {
                    taskList.closure(url, Terminal.execYoutubeDL(url, youtubeDLPath));
                }
            });
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
