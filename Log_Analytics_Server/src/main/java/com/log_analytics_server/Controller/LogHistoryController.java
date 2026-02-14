package com.log_analytics_server.Controller;

import com.log_analytics_server.Model.BuildLogEntity;
import com.log_analytics_server.Repository.BuildLogRepository;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

/**
 * Provides a REST endpoint for querying historical build logs from PostgreSQL.
 * This supplements the SSE real-time streaming endpoint by providing completed
 * deployment logs that may no longer be in Redis Streams.
 */
@RestController
@RequestMapping("/api/logs")
public class LogHistoryController {

    private final BuildLogRepository buildLogRepository;

    public LogHistoryController(BuildLogRepository buildLogRepository) {
        this.buildLogRepository = buildLogRepository;
    }

    @GetMapping("/history/{deploymentId}")
    public Flux<BuildLogEntity> getLogHistory(@PathVariable Long deploymentId) {
        return buildLogRepository.findByDeploymentIdOrderByTimestampAsc(deploymentId);
    }
}
