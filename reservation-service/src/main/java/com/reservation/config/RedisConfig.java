package com.reservation.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate redisTemplate() {
        LettuceConnectionFactory factory = new LettuceConnectionFactory();
        
        RedisTemplate template = new RedisTemplate();
        template.setConnectionFactory(factory);

        return template;
    }
}