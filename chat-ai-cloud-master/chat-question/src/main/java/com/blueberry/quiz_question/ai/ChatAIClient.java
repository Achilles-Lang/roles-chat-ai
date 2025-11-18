package com.blueberry.quiz_question.ai;

import com.blueberry.model.quetion.QuestionDTO;
import com.blueberry.quiz_question.model.ChatRequest;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class ChatAIClient {
    private static final String SYSTEM_PROMPT = "请扮演一个专业的知识题库答疑 AI，能够针对各类知识题目进行答疑";

    private final ChatClient chatClient;

    public ChatAIClient(ChatModel model) {
        ChatMemory chatMemory = new InMemoryChatMemory();
        chatClient = ChatClient.builder(model)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(
                        new MessageChatMemoryAdvisor(chatMemory)
                )
                .build();
    }

    /**
     * 返回流式传输的问答方法
     * @param chatRequest 请求
     * @return 流式回答
     */
    public Flux<ChatResponse> StreamChat(ChatRequest chatRequest) {
        QuestionDTO question = chatRequest.getQuestion();
        var context = "在这轮对话中，你只能回答与该问题相关的问题，而不能回答其他问题" +
                System.lineSeparator() + "该问题的题目为:" +
                question.getTitle() +
                "答案为:" +
                question.getAnswers().toString() +
                System.lineSeparator() +
                "正确答案为:" +
                question.getRightAnswer();
        return chatClient.prompt().system(context).user(chatRequest.getUserQuestion()).stream().chatResponse();
    }
}
