package com.blueberry.quiz_user;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
@MapperScan("com.blueberry.quiz_user.mapper")
public class ChatUserApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChatUserApplication.class, args);
    }

}
