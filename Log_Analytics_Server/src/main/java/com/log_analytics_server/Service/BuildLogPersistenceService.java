package com.log_analytics_server.Service;

import com.log_analytics_server.Model.BuildLogEntity;
import com.log_analytics_server.Repository.BuildLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
public class BuildLogPersistenceService {

    private final BuildLogRepository repository;
    private final ReactiveRedisTemplate<String, String> redisTemplate;

    @Autowired
    public BuildLogPersistenceService(
            BuildLogRepository repository,
            @Qualifier("reactiveRedisTemplate") ReactiveRedisTemplate<String, String> redisTemplate) {
        this.repository = repository;
        this.redisTemplate = redisTemplate;
    }

    /**
     * Periodically scan for log streams in Redis and flush them to PostgreSQL
     * if they contain the "BUILD_COMPLETED" signal.
     */
    @Scheduled(fixedDelay = 60000) // Every 1 minute
    public void scanAndFlushLogs() {
        System.out.println("[BuildLogPersistenceService] Scanning Redis for completed logs...");

        redisTemplate.keys("logs:*")
                .flatMap(this::processStreamIfCompleted)
                .subscribe(
                    buildId -> System.out.println("[BuildLogPersistenceService] Successfully persisted logs for deployment: " + buildId),
                    error -> System.err.println("[BuildLogPersistenceService] Error persisting logs: " + error.getMessage())
                );
    }

    private Mono<String> processStreamIfCompleted(String streamKey) {
        String deploymentIdStr = streamKey.substring("logs:".length());
        Long deploymentId;
        try {
            deploymentId = Long.parseLong(deploymentIdStr);
        } catch (NumberFormatException e) {
            return Mono.empty();
        }

        return redisTemplate.opsForStream()
                .reverseRange(streamKey, Range.unbounded())
                .next() // Get the latest record
                .flatMap(record -> {
                    Object lastLogObj = record.getValue().get("log");
                    String lastMessage = lastLogObj != null ? lastLogObj.toString() : null;
                    if ("BUILD_COMPLETED".equals(lastMessage)) {
                        System.out.println("[BuildLogPersistenceService] Found completed build: " + deploymentId);
                        return flushToDatabase(streamKey, deploymentId)
                                .then(redisTemplate.delete(streamKey))
                                .thenReturn(deploymentIdStr);
                    }
                    return Mono.empty();
                });
    }

    private Mono<Void> flushToDatabase(String streamKey, Long deploymentId) {
        return redisTemplate.opsForStream()
                .range(streamKey, Range.unbounded())
                .filter(record -> {
                    Object logObj = record.getValue().get("log");
                    return !"BUILD_COMPLETED".equals(logObj != null ? logObj.toString() : null);
                })
                .map(record -> {
                    BuildLogEntity entity = new BuildLogEntity();
                    entity.setDeploymentId(deploymentId);
                    Object logObj = record.getValue().get("log");
                    entity.setLog(logObj != null ? logObj.toString() : "");
                    entity.setType("INFO");
                    // Use a default timestamp if not available in the record
                    entity.setTimestamp(LocalDateTime.now());
                    return entity;
                })
                .flatMap(repository::save)
                .then();
    }
}
