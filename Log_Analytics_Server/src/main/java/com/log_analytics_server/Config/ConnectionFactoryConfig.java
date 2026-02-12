package com.log_analytics_server.Config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

@Configuration
public class ConnectionFactoryConfig {

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private int redisPort;

    @Value("${spring.data.redis.username:}")
    private String redisUsername;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Value("${spring.data.redis.ssl.enabled:false}")
    private boolean redisSslEnabled;

    @Primary
    @Bean
    public ReactiveRedisConnectionFactory reactiveRedisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(redisHost);
        config.setPort(redisPort);
        if (redisUsername != null && !redisUsername.isBlank()) {
            config.setUsername(redisUsername);
        }
        if (redisPassword != null && !redisPassword.isBlank()) {
            config.setPassword(redisPassword);
        }

        LettuceClientConfiguration clientConfig;
        if (redisSslEnabled) {
            clientConfig = LettuceClientConfiguration.builder()
                    .useSsl()
                    .build();
        } else {
            clientConfig = LettuceClientConfiguration.defaultConfiguration();
        }

        return new LettuceConnectionFactory(config, clientConfig);
    }

}
