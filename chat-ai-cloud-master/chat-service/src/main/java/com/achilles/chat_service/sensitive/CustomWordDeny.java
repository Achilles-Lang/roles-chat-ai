package com.achilles.chat_service.sensitive;

import com.github.houbb.sensitive.word.api.IWordDeny;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author Achilles
 * 自定义敏感词黑名单
 */
@Component
public class CustomWordDeny implements IWordDeny {
    @Override
    public List<String> deny() {
        return List.of("伟人","锦涛");
    }
}
