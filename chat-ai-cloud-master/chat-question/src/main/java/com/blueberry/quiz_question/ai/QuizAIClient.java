package com.blueberry.quiz_question.ai;

import com.blueberry.model.quetion.QuestionDTO;
import com.blueberry.quiz_question.model.QuestionRequest;
import com.blueberry.model.quetion.QuizDataDTO;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

/**
 * 用于题库生成的大模型客户端
 */
@Component
public class QuizAIClient {
    private final ChatClient chatClient;

    private static final String SYSTEM_PROMPT =
            "请扮演一个专业的编程知识题库生成 AI，能够针对各类编程语言和计算机科学主题创建高质量、多层次的练习题。" +
                    "你的任务是生成结构规范、知识点覆盖全面、难度分级清晰的编程题库，满足不同学习阶段用户的练习需求";
    private static final String QUESTION_PROMPT =
            "生成一个用户所描述的问题，标题为问题的标题，内容为该问题的四个选项，包含正确答案和问题解释等。要求：" +
                    "生成中文题目，问题和选项设置尽量简单易懂";
    private static final String QUESTION_BANK_PROMPT =
            "生成一个题库，要求生成题库名称、描述和总题数，题库中的所有题目都要围绕着题库的要求来生成" +
                    "题库有一个总体难度，但题目难度要按照一个比例来进行分布";

    public QuizAIClient(ChatModel model) {
        ChatMemory chatMemory = new InMemoryChatMemory();
        chatClient = ChatClient.builder(model)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(
                        new MessageChatMemoryAdvisor(chatMemory)
                )
                .build();
    }

    public QuestionDTO createQuestion(String content) {
        return chatClient
                .prompt()
                .system(QUESTION_PROMPT)
                .user(content)
                .call()
                .entity(QuestionDTO.class);
    }

    public QuizDataDTO generateQuestionBank(QuestionRequest questionBank) {
        var content = "主题为:" + questionBank.getName() +
                "总题数为:" + questionBank.getQuestionCount() +
                "总难度为:" + questionBank.getDifficulty();
        return chatClient
                .prompt()
                .system(QUESTION_BANK_PROMPT + QUESTION_PROMPT)
                .user(content)
                .call()
                .entity(QuizDataDTO.class);
    }
}
