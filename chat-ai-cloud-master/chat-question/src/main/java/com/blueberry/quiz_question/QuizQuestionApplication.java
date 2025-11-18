package com.blueberry.quiz_question;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@MapperScan("com.blueberry.quiz_question.mapper")
@SpringBootApplication
public class QuizQuestionApplication {

    public static void main(String[] args) {
        SpringApplication.run(QuizQuestionApplication.class, args);
    }

}
