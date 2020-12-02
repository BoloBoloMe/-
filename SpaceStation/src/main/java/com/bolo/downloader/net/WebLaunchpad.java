//package com.bolo.downloader.station;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RequestParam;
//import org.springframework.web.bind.annotation.RestController;
//
//import java.util.*;
//
//@RestController
//public class WebLaunchpad {
//    @Autowired
//    private Downloader downloader;
//
//    @PostMapping("task/add")
//    public ResponseEntity<String> addTask(@RequestParam("url") String url) {
//        return ResponseEntity.status(HttpStatus.OK).body(downloader.addTask(new String(Base64.getDecoder().decode(url))) == 1 ? "添加成功" : "任务已在列表中！");
//    }
//
//    @RequestMapping("task/clear")
//    public ResponseEntity<String> clearTasks() {
//        downloader.clearTasks();
//        return ResponseEntity.status(HttpStatus.OK).body("列表已清空");
//    }
//
//    @RequestMapping("task/list")
//    public ResponseEntity<Map<String, String>> listTask() {
//        return ResponseEntity.status(HttpStatus.OK).body(downloader.listTasks());
//    }
//
//    @RequestMapping("video/list")
//    public ResponseEntity<List<String>> listVideo() {
//        return ResponseEntity.status(HttpStatus.OK).body(downloader.listVideo());
//    }
//}
