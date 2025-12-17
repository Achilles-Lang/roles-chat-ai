package com.achilles.chat_service.ai;

import com.achilles.model.common.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;

@FeignClient(name = "chat-knowledge") // 指定调用 chat-knowledge 服务
public interface KnowledgeClient {

    @GetMapping("/knowledge/search")
    Result<List<String>> search(@RequestParam("query") String query);
}
