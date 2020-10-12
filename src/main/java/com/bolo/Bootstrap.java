package com.bolo;

import com.bolo.downloader.Terminal;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@ComponentScan("com.bolo")
@SpringBootApplication
public class Bootstrap {
    public static void main(String[] args) {
        Terminal.execYoutubeDL("https://vod.300hu.com/4c1f7a6atransbjngwcloud1oss/29d26437280639959563001857/v.f30.mp4?dockingId=1f8abff1-05d7-4f60-809b-e4523795eb341", "/home/dev/视频/{}");
        SpringApplication.run(Bootstrap.class);
    }
}
