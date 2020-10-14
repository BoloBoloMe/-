package com.bolo.downloader;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
public class WebLaunchpad {
    @Autowired
    private Downloader downloader;

    @PostMapping("task/add")
    public ResponseEntity<String> addTask(@RequestParam("url") String url) {
        return ResponseEntity.status(HttpStatus.OK).body(downloader.addTask(new String(Base64.getDecoder().decode(url))) == 1 ? "添加成功" : "任务已在列表中！");
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
    public ResponseEntity<Map<String, Object>> listTask() {
        Map<String, String> list = downloader.listTasks();
        // {"code":0,"msg":"",data:[{}]}
        Map<String, Object> result = new HashMap<>();
        result.put("code", "0");
        result.put("msg", "成功");
        List<Map<String, String>> data = new ArrayList<>();
        for (String url : list.keySet()) {
            Map<String, String> item = new HashMap<>();
            item.put("url", url);
            item.put("status", list.get(url));
            data.add(item);
        }
        result.put("data", data);
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

    @RequestMapping("video/list")
    public ResponseEntity<Map<String, Object>> listVideo() {
        // {"code":0,"msg":"",data:[{}]}
        Map<String, Object> result = new HashMap<>();
        result.put("code", "0");
        result.put("msg", "成功");
        result.put("data", downloader.listVideo().stream().map(file -> {
            HashMap<String, String> item = new HashMap<>();
            item.put("fileName", file);
            return item;
        }));
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }
}
