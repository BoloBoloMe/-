package com.bolo.downloader;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class WebLaunchpad {
    @Autowired
    private Downloader downloader;

    @RequestMapping("echo")
    public ResponseEntity echo() {
        return ResponseEntity.status(HttpStatus.OK).body(null);
    }

    @PostMapping("task/add")
    public ResponseEntity<String> addTask(@RequestParam("url") String url) {
        return ResponseEntity.status(HttpStatus.OK).body(downloader.addTask(url) == 1 ? "添加成功" : "任务已在列表中！");
    }

    @RequestMapping("task/start")
    public ResponseEntity<String> startTask() {
        return ResponseEntity.status(HttpStatus.OK).body(downloader.startTask() == 2 ? "任务启动失败！" : "任务已开始处理...");
    }

    @RequestMapping("task/clear")
    public ResponseEntity<String> clearTasks() {
        downloader.clearTasks();
        return ResponseEntity.status(HttpStatus.OK).body("列表已清空");
    }

    @RequestMapping("task/list")
    public ResponseEntity<Map<String, String>> listTask() {
        return ResponseEntity.status(HttpStatus.OK).body(downloader.list());
    }


}
