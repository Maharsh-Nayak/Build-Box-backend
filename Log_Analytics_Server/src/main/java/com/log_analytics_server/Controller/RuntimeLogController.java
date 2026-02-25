package com.log_analytics_server.Controller;

import com.log_analytics_server.Service.RuntimeLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/v2/runtime")
//@CrossOrigin(origins = "localhost:5173", allowedHeaders = "*")
public class RuntimeLogController {

    @Autowired
    private RuntimeLogService runtimeLogService;

    @GetMapping(value = "/{projectId}/logs", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamRuntimeLogs(
            @PathVariable String projectId,
            @RequestHeader(value = "Last-Event-ID", required = false) String lastEventId) {

        System.out.println(projectId);

        Flux<ServerSentEvent<String>> heartbeat = Flux.interval(java.time.Duration.ofSeconds(15))
                .map(i -> ServerSentEvent.<String>builder()
                        .event("heartbeat")
                        .data("keep-alive")
                        .build());

        Flux<ServerSentEvent<String>> logs = runtimeLogService.streamRuntimeLogs(projectId, lastEventId)
                .map(record -> {
                    String log = record.getValue().getOrDefault("log", "");
                    String source = record.getValue().getOrDefault("source", "runtime");
                    String data = source + "|" + log;

                    return ServerSentEvent.<String>builder()
                            .id(record.getId().getValue())
                            .event("log")
                            .data(data)
                            .build();
                });

        ServerSentEvent<String> initial = ServerSentEvent.<String>builder()
                .event("system")
                .data("system|✅ Log analytics connection established")
                .build();

        return Flux.concat(Flux.just(initial), Flux.merge(logs, heartbeat));
    }
}
