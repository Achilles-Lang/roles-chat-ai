package com.achilles.chat_service.redis;

import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class RedisManager {
    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private Gson gson;

    // 存储对象（自动序列化为JSON）
    public <T> void setObject(String key, T value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(
                key,
                gson.toJson(value),
                timeout,
                unit
        );
    }

    // 获取对象（自动反序列化为指定类型）
    public <T> T getObject(String key, Class<T> clazz) {
        String json = redisTemplate.opsForValue().get(key);
        return gson.fromJson(json, clazz);
    }

    // 删除键
    public Boolean deleteKey(String key) {
        return redisTemplate.delete(key);
    }
}
