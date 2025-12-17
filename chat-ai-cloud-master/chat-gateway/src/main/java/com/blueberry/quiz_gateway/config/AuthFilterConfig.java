package com.blueberry.quiz_gateway.config;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

@Configuration
public class AuthFilterConfig {

    private final StringRedisTemplate redisTemplate;

    public AuthFilterConfig(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Bean
    @Order(1)
    public GlobalFilter authFilter() {
        return new AuthFilter();
    }

    private Mono<Void> Unauthorized(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().add("Content-Type", "application/json");
        String json = "{\"code\":401,\"message\":\"" + message + "\"}";
        return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory().wrap(json.getBytes()))
        );
    }

    class AuthFilter implements GlobalFilter {
        @Override
        public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
            String path = exchange.getRequest().getPath().value();
            List<String> publicPaths = List.of("/user/login", "/user/register");
            if (publicPaths.stream().anyMatch(path::startsWith)) {
                return chain.filter(exchange); // 直接放行
            }
            //从请求头中获取Token
            String token = exchange.getRequest().getHeaders().getFirst("Authorization");
            if (token == null || token.isEmpty()) {
                return Unauthorized(exchange, "未携带Token");
            }
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            String userId = redisTemplate.opsForValue().get(token);
            if (userId == null) {
                return Unauthorized(exchange, "Token无效");
            }
            //更新过期时间
            redisTemplate.expire(token, Duration.ofHours(2));

            var request = exchange.getRequest().mutate()
                    .header("userId", userId)
                    .build();
            return chain.filter(exchange.mutate().request(request).build());
        }
    }
}
