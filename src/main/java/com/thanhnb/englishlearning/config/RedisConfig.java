package com.thanhnb.englishlearning.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis Configuration cho JWT Blacklist
 * ⚠️ BỎ @EnableRedisHttpSession vì đang dùng JWT (stateless)
 */
@Configuration
// ⚠️ BỎ annotation này - không cần session cho JWT
// @EnableRedisHttpSession(maxInactiveIntervalInSeconds = 1800)
public class RedisConfig {

    /**
     * Tạo connection factory để kết nối Redis
     * Mặc định localhost:6379, có thể config từ application.properties
     */
    @Bean
    public RedisConnectionFactory connectionFactory() {
        return new LettuceConnectionFactory(new RedisStandaloneConfiguration("localhost", 6379));
    }

    /**
     * RedisTemplate để thao tác với Redis
     * Cấu hình serializer để lưu trữ object dưới dạng JSON
     * 
     * Dùng cho:
     * - JWT Blacklist (logout tokens)
     * - Cache nếu cần
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // Sử dụng String cho key, JSON cho value
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        
        return template;
    }
}