package com.blueberry.quiz_question.service.api;

import com.blueberry.quiz_question.model.ChatRequest;
import org.springframework.ai.chat.model.ChatResponse;
import reactor.core.publisher.Flux;

public interface ChatService {
    Flux<ChatResponse> StreamChat(ChatRequest chatRequest);
}
