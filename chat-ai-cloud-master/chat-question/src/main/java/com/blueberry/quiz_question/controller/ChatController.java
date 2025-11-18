package com.blueberry.quiz_question.controller;

import com.blueberry.quiz_question.model.ChatRequest;
import com.blueberry.quiz_question.service.api.ChatService;
import com.google.gson.Gson;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("question")
public class ChatController {

    @Autowired
    private ChatService chatService;

    /**
     * Stream 流式调用。可以使大模型的输出信息实现打字机效果。
     *
     * @return Flux<String> types.
     */
    @PostMapping("/stream/chat")
    public Flux<String> streamChat(@RequestBody ChatRequest request, HttpServletResponse response) {
        // 避免返回乱码
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json; charset=UTF-8"); // 声明为JSON格式
        response.setHeader("Cache-Control", "no-cache"); // 禁止缓存，确保流式实时性
        response.setHeader("Connection", "keep-alive");

        Flux<ChatResponse> stream = chatService.StreamChat(request);

        return stream.handle((resp, sink) -> {
            try {
                // 将AssistantMessage对象转为JSON字符串
                sink.next(new Gson().toJson(resp.getResult().getOutput()));
            } catch (Exception e) {
                // 处理序列化异常（实际项目中建议更完善的异常处理）
                sink.error(new RuntimeException("序列化响应失败", e));
            }
        });
    }
}
