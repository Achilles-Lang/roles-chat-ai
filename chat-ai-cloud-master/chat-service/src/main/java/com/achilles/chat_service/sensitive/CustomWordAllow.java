package com.achilles.chat_service.sensitive;

import com.github.houbb.sensitive.word.api.IWordAllow;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CustomWordAllow implements IWordAllow {
    @Override
    public List<String> allow() {
        return List.of("允许的词1","允许的词2");
    }
}
