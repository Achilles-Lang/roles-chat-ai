package com.achilles.chatknowledge.service;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Files;
import java.nio.file.Paths;

@Configuration
public class RagConfig {

    @Bean
    public EmbeddingStore<TextSegment> embeddingStore() {
        // 定义向量库文件的存储路径
        String filePath = "./vector-store.json";
        if (Files.exists(Paths.get(filePath))) {
            // 如果文件存在，说明之前存过数据，从文件加载
            System.out.println(">>> 发现本地向量库文件，正在加载: " + filePath);
            return InMemoryEmbeddingStore.fromFile(filePath);
        } else {
            // 如果文件不存在（第一次运行），创建一个新的空仓库
            System.out.println(">>> 本地向量库文件不存在，创建新的空仓库");
            return new InMemoryEmbeddingStore<>();
        }
    }
}