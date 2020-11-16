package com.bolo.downloader;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@ComponentScan("com.bolo")
@SpringBootApplication
public class Bootstrap {
    public static void main(String[] args) {
        SpringApplication.run(Bootstrap.class);
    }
}
