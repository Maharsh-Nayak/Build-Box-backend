package com.BuildBox.BuildServer.configuration;

import com.BuildBox.BuildServer.model.TaskInfo;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, TaskInfo> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, TaskInfo> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Key is String
        template.setKeySerializer(new StringRedisSerializer());

        // Configure ObjectMapper for JSON serialization
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule()); // Support for Instant
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);

        // Value is TaskInfo (JSON)
        Jackson2JsonRedisSerializer<TaskInfo> serializer = new Jackson2JsonRedisSerializer<>(mapper, TaskInfo.class);
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(serializer);

        template.afterPropertiesSet();
        return template;
    }
}
