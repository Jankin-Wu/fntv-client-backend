package com.jankinwu.fntv.client;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@MapperScan("com.jankinwu.fntv.client.mapper")
@SpringBootApplication
public class FntvDesktopBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(FntvDesktopBackendApplication.class, args);
    }

}
