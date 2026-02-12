package com.log_analytics_server.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/v2/buildLogs")
@CrossOrigin(origins = "http://localhost:8080", allowedHeaders = "*", allowCredentials = "true")
public class LogControllerV2 {

    private ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

    @Autowired
    public LogControllerV2(ReactiveRedisTemplate<String, String> reactiveRedisTemplate) {
        this.reactiveRedisTemplate = reactiveRedisTemplate;
    }

    @GetMapping(value = "/{buildId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> getBuildLogs(@PathVariable String buildId) {
        String key = "logs:"+buildId;

        System.out.println(key);

        return reactiveRedisTemplate.opsForStream()
                .read(StreamOffset.fromStart(key))
                .map(record -> {

                    System.out.println(record);

                    String logLine = record.getValue().get("log").toString();
                    String status = "IN_PROGRESS";

                    if (logLine.contains("__BUILD_STATUS__:SUCCESS"))
                        status = "SUCCESS";

                    if (logLine.contains("__BUILD_STATUS__:FAILED"))
                        status = "FAILED";

                    String json = String.format(
                            "{\"logs\": [\"%s\"], \"status\": \"%s\"}",
                            logLine.replace("\"", "\\\""),
                            status
                    );

//                    System.out.println(json);

                    return ServerSentEvent.<String>builder()
                            .data(json)
                            .build();
                });
    }

}
