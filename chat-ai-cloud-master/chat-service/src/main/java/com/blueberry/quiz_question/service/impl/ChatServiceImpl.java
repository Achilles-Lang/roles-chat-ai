package com.blueberry.quiz_question.service.impl;

import com.blueberry.quiz_question.ai.ChatAIClient;
import com.blueberry.quiz_question.model.ChatRequest;
import com.blueberry.quiz_question.service.api.ChatService;
import com.github.houbb.sensitive.word.bs.SensitiveWordBs;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;

@Service
public class ChatServiceImpl implements ChatService {
    @Autowired
    private ChatAIClient chatAIClient;

    @Autowired
    private SensitiveWordBs sensitiveWordBs;

    @Override
    public Flux<ChatResponse> StreamChat(ChatRequest chatRequest){
        List<String> foundWords = sensitiveWordBs.findAll(chatRequest.getUserQuestion());

        if(!foundWords.isEmpty()){
            String firstFoundWord = foundWords.get(0);
            throw new IllegalArgumentException("输入信息包含敏感词["+firstFoundWord+"]等，请修改后重试");
        }
        return chatAIClient.StreamChat(chatRequest);
    }
}
