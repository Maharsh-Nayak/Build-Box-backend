package com.log_analytics_server.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.stream.StreamReceiver;

import java.time.Duration;

@Configuration
public class StreamConfig {

    @Bean
    public StreamReceiver<String, MapRecord<String,String,String>> streamReceiver(
            ReactiveRedisConnectionFactory factory) {

        StreamReceiver.StreamReceiverOptions<String, MapRecord<String,String,String>> options =
                StreamReceiver.StreamReceiverOptions.builder()
                        .pollTimeout(Duration.ofSeconds(1))
                        .build();

        return StreamReceiver.create(factory, options);
    }
}
