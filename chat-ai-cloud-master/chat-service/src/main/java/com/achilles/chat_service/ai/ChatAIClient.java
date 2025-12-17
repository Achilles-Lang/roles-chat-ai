package com.achilles.chat_service.ai;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesis;
import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesisParam;
import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesisResult;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Map;


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
    /**
     * AI 绘画方法
     * @param apiKey 用户自定义 Key
     * @param prompt 提示词
     * @return 图片的 URL
     */
    public String generateImage(String apiKey, String prompt) {
        try {
            // 1. 准备 Key (优先用用户的)
            String effectiveApiKey = StringUtils.hasText(apiKey) ? apiKey : defaultApiKey;

            // 2. 组装绘画请求
            ImageSynthesisParam param = ImageSynthesisParam.builder()
                    .apiKey(effectiveApiKey)
                    .model(ImageSynthesis.Models.WANX_V1) // 指定模型：通义万相
                    .prompt(prompt)      // 画什么
                    .n(1)                // 画几张
                    .size("1024*1024")   // 分辨率
                    .build();

            // 3. 开始绘画
            ImageSynthesis imageSynthesis = new ImageSynthesis();
            ImageSynthesisResult result = imageSynthesis.call(param);

            // 4. 解析结果
            if (result != null
                    && result.getOutput() != null
                    && !CollectionUtils.isEmpty(result.getOutput().getResults())) {
                // 获取第1个结果（Map类型）
                Map<String, String> firstResult = (Map<String, String>) result.getOutput().getResults().get(0);
                // 通过"url"键获取图片链接
                String imageUrl = firstResult.get("url");
                // 非空判断
                if (StringUtils.hasText(imageUrl)) {
                    return imageUrl;
                }
                return "（绘画失败：未获取到图片链接）";
            }
            return "（绘画失败：AI 没给图）";

        } catch (Exception e) {
            e.printStackTrace();
            return "（绘画出错：" + e.getMessage() + "）";
        }
    }
}
