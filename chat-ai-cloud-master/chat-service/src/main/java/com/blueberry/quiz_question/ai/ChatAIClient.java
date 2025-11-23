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
    private static final String SYSTEM_PROMPT = "你是一个热情、幽默的群聊助手。你的任务是活跃气氛，" +
            "回答用户的问题，或者对用户的发言进行有趣的接话。" +
            "回复尽量简短（50字以内），不要长篇大论。";

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

    public String chat(String uerMessage){
        try {
            return chatClient.prompt()
                    .user(uerMessage)
                    .call()
                    .content();
        } catch (Exception e) {
            e.printStackTrace();
            return "我无法回答你的问题";
        }

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
