package com.bolo.downloader;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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


}
