package com.buildbox_backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/v2/buildLogs")
public class LogControllerV2 {

    private ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

    @Autowired
    public LogControllerV2(ReactiveRedisTemplate<String, String> reactiveRedisTemplate) {
        this.reactiveRedisTemplate = reactiveRedisTemplate;
    }

    @GetMapping(value = "/{buildId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> getBuildLogs(@PathVariable String buildId) {
        String key = "logs:"+buildId;

        System.out.println(key);

        return reactiveRedisTemplate.opsForStream().read(StreamOffset.fromStart(key)).map(record->{ return record.getValue().get("log").toString(); });
    }

}
