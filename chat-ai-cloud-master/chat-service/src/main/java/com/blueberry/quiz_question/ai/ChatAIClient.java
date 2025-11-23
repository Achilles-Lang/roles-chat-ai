package com.blueberry.quiz_question.ai;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.blueberry.model.quetion.QuestionDTO;
import com.blueberry.quiz_question.model.ChatRequest;
import opennlp.tools.util.StringUtil;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

@Component
public class ChatAIClient {
    // 默认模型
    private final ChatClient defaultChatClient;
    // 默认模型API KEY
    @Value("${spring.ai.dashscope.api-key")
    private String defaultApiKey;

    public ChatAIClient(ChatModel model){
        this.defaultChatClient = ChatClient.builder(model).build();
    }

    public String chatDynamic(String apiKey,String modelName,String systemPrompt,String userMessage){
        try {
            ChatClient clientToUse;
            // 如果用户自带了key，则使用专属的AI连接器
            if(StringUtils.hasText(apiKey)){
                System.out.println("使用用户自定义 Key 调用："+modelName);
                // 用户的api
                DashScopeApi api = new DashScopeApi(apiKey);
                // 创建模型并指定名字
                DashScopeChatModel customModel = new DashScopeChatModel( api,
                        DashScopeChatOptions.builder()
                                .withModel(StringUtils.hasText(modelName)? modelName : "qwen-plus")
                                .build());
                clientToUse = ChatClient.builder(customModel).build();
            } else {
                // 如果没带 key，则使用默认的
                clientToUse = defaultChatClient;
            }
            // 发起请求
            return clientToUse.prompt()
                    .system(systemPrompt)
                    .user(userMessage)
                    .call()
                    .content();
        } catch (Exception e){
            e.printStackTrace();
            return "(AI连接失败："+e.getMessage()+")";
        }
    }

    public String chat(String userMessage){
         return chatDynamic(null,null,"你是一个助手",userMessage);
    }
    public String chatWithPersona(String systemPrompt,String userMessage){
        return chatDynamic(null,null,systemPrompt,userMessage);
    }
}
