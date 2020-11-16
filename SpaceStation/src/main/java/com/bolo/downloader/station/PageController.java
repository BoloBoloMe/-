package com.bolo.downloader.station;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class PageController {
    @GetMapping("hello")
    public String hello() {
        return "page/hello";
    }


    @RequestMapping("v")
    public String jump(@RequestParam(value = "p") String page) {
        return "page/" + page;
    }
}
