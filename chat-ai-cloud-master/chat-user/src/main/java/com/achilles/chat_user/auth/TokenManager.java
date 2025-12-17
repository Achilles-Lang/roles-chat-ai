package com.achilles.chat_user.auth;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

@Component
public class TokenManager {
    private final StringRedisTemplate redisTemplate;

    // 构造方法注入配置参数和Redis模板
    public TokenManager(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public String generateToken(long userId) {
        //生成token之前先清除之前未过期的token
        clearToken(userId);
        String token = UUID.randomUUID().toString().replaceAll("-", "");
        redisTemplate.opsForValue().set(token, String.valueOf(userId), Duration.ofHours(2)); //用于验证token
        redisTemplate.opsForValue().set(String.valueOf(userId), token, Duration.ofHours(2)); //用于防止冗余token
        return token;
    }

    /**
     * 清空redis中存储的 token
     * 包括 userId->token 和 token->userId
     */
    public void clearToken(long userId) {
        var token = redisTemplate.opsForValue().get(String.valueOf(userId));
        if (token != null) {
            redisTemplate.delete(token);
            redisTemplate.delete(String.valueOf(userId));
        }
    }

    public String verifyToken(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token == null || token.isEmpty()) {
            return null;
        }
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        return redisTemplate.opsForValue().get(token);
    }
}
