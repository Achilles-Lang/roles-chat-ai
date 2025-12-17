package com.achilles.chatknowledge.service;

import com.achilles.chatknowledge.entity.KnowledgeDocument;
import com.achilles.chatknowledge.mapper.KnowledgeMapper;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.dashscope.QwenEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class KnowledgeService {

    @Resource
    private KnowledgeMapper knowledgeMapper;

    @Resource
    private EmbeddingStore<TextSegment> embeddingStore;

    @Value("${file.upload-path:./uploads/}")
    private String uploadPath;

    // 直接在代码里读取配置的 API Key
    @Value("${langchain4j.dashscope.api-key}")
    private String apiKey;

    /**
     * 处理上传的文件
     */
    public void processUpload(MultipartFile file, String description, Long userId) {
        try {
            File directory = new File(uploadPath);
            if (!directory.exists()) {
                directory.mkdirs();
            }
            String finalName = file.getOriginalFilename();
            File saveFile = new File(directory, finalName);

            file.transferTo(saveFile);

            // 记录到数据库
            KnowledgeDocument doc = new KnowledgeDocument();
            doc.setUserId(userId);
            doc.setFileName(file.getOriginalFilename());
            doc.setDescription(description);
            doc.setCreateTime(LocalDateTime.now());
            doc.setFileUrl(saveFile.getAbsolutePath());
            knowledgeMapper.insert(doc);

            // 使用 LangChain4j 解析文件
            Document document = FileSystemDocumentLoader.loadDocument(saveFile.toPath());

            // 初始化 Embedding 模型
            QwenEmbeddingModel embeddingModel = QwenEmbeddingModel.builder()
                    .apiKey(apiKey)
                    .modelName("text-embedding-v2")
                    .build();

            // 切分文档 + 向量化 + 存入向量库
            dev.langchain4j.store.embedding.EmbeddingStoreIngestor ingestor = dev.langchain4j.store.embedding.EmbeddingStoreIngestor.builder()
                    .documentSplitter(DocumentSplitters.recursive(300, 20))
                    .embeddingModel(embeddingModel)
                    .embeddingStore(embeddingStore)
                    .build();

            ingestor.ingest(document);


        } catch (IOException e) {
            throw new RuntimeException("文件处理失败: " + e.getMessage());
        }
    }

    /**
     * 根据用户问题，搜索相关的知识片段
     */
    public List<String> search(String queryText) {
        QwenEmbeddingModel embeddingModel = QwenEmbeddingModel.builder()
                .apiKey(apiKey)
                .modelName("text-embedding-v2")
                .build();

        Embedding queryEmbedding = embeddingModel.embed(queryText).content();
        List<EmbeddingMatch<TextSegment>> matches = embeddingStore.findRelevant(queryEmbedding, 3, 0.6);

        return matches.stream()
                .map(match -> match.embedded().text())
                .collect(Collectors.toList());
    }
}
