package com.log_analytics_server.Controller;

import com.log_analytics_server.Service.BuildLogsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/v2/buildLogs")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class LogControllerV2 {

        @Autowired
        private BuildLogsService logService;

        @GetMapping(value = "/{id}/logs", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
        public Flux<ServerSentEvent<String>> streamLogs(
                        @PathVariable("id") String buildId,
                        @RequestHeader(value = "Last-Event-ID", required = false) String lastEventId) {

                System.out.println(buildId);
                System.out.println(lastEventId);

                return logService.streamLogs(buildId, lastEventId)
                                .map(record -> {

                                        String message = record.getValue().getOrDefault("log",
                                                        record.getValue().get("message"));
                                        if (message == null) {
                                                message = "";
                                        }

                                        return ServerSentEvent.<String>builder()
                                                        .id(record.getId().getValue())
                                                        .event("log")
                                                        .data(message)
                                                        .build();
                                });
        }

}
