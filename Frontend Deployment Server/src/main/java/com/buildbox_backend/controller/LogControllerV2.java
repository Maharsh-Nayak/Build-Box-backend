package com.buildbox_backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/V2/buildLogs")
public class LogControllerV2 {

    private ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

    @Autowired
    public LogControllerV2(ReactiveRedisTemplate<String, String> reactiveRedisTemplate) {
        this.reactiveRedisTemplate = reactiveRedisTemplate;
    }

//    public

}
