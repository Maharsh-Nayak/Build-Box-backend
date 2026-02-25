package com.log_analytics_server.Service;

import com.log_analytics_server.Model.AnalyticsEventEntity;
import com.log_analytics_server.Repository.AnalyticsEventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.stream.StreamReceiver;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import jakarta.annotation.PostConstruct;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class AnalyticsEventConsumerService {

    private final AnalyticsEventRepository repository;
    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final StreamReceiver<String, MapRecord<String, String, String>> receiver;

    @Autowired
    public AnalyticsEventConsumerService(
            AnalyticsEventRepository repository,
            @Qualifier("reactiveRedisTemplate") ReactiveRedisTemplate<String, String> redisTemplate,
            StreamReceiver<String, MapRecord<String, String, String>> receiver) {
        this.repository = repository;
        this.redisTemplate = redisTemplate;
        this.receiver = receiver;
    }

    @PostConstruct
    public void startConsuming() {
        System.out.println("[AnalyticsEventConsumerService] Starting analytics event consumer...");
        consumeAnalyticsEventsHistory()
                .doOnNext(event -> System.out.println("[AnalyticsEventConsumerService] Saved event: " + event.getId()))
                .doOnError(error -> {
                    System.err.println("[AnalyticsEventConsumerService] Consumer error: " + error.getMessage());
                    error.printStackTrace();
                })
                .doOnComplete(() -> System.out.println("[AnalyticsEventConsumerService] History consumer completed"))
                .subscribe(
                    event -> System.out.println("[AnalyticsEventConsumerService] Event processed: " + event.getId()),
                    error -> {
                        System.err.println("[AnalyticsEventConsumerService] FATAL ERROR in subscribe: " + error.getMessage());
                        error.printStackTrace();
                    },
                    () -> System.out.println("[AnalyticsEventConsumerService] Consumer subscription completed")
                );
    }

    public Flux<AnalyticsEventEntity> consumeAnalyticsEventsHistory() {
        String streamKey = "analytics-events";

        // Get history of events - one time batch
        return redisTemplate.opsForStream()
                .range(streamKey, Range.unbounded())
                .doOnNext(record -> System.out.println("[AnalyticsEventConsumerService] Found redis record: " + record.getId() + " with " + record.getValue().size() + " fields - " + record.getValue()))
                .map(record -> {
                    try {
                        Map<String, String> stringMap = new HashMap<>();
                        record.getValue().forEach((k, v) -> {
                            String key = k.toString();
                            String value = v.toString();
                            stringMap.put(key, value);
                        });
                        System.out.println("[AnalyticsEventConsumerService] Converted Objects to Strings. Keys: " + stringMap.keySet());
                        return MapRecord.create(record.getStream(), stringMap)
                                .withId(record.getId());
                    } catch (Exception e) {
                        System.err.println("[AnalyticsEventConsumerService] Error converting record: " + e.getMessage());
                        e.printStackTrace();
                        throw new RuntimeException(e);
                    }
                })
                .doOnError(e -> System.err.println("[AnalyticsEventConsumerService] Error in map step: " + e.getMessage()))
                .flatMap(this::mapRecordToEntity)
                .doOnError(e -> System.err.println("[AnalyticsEventConsumerService] Error in mapRecordToEntity: " + e.getMessage()))
                .flatMap(entity -> {
                    System.out.println("[AnalyticsEventConsumerService] SAVING entity to database: " + entity.getId() + " (projectId=" + entity.getProjectId() + ")");
                    return repository.save(entity)
                        .doOnSuccess(saved -> System.out.println("[AnalyticsEventConsumerService] ✅ DATABASE SAVE SUCCESSFUL: " + saved.getId()))
                        .doOnError(error -> System.err.println("[AnalyticsEventConsumerService] ❌ DATABASE SAVE FAILED: " + error.getMessage() + " | " + error.getClass().getSimpleName()))
                        .doOnError(error -> error.printStackTrace())
                        .onErrorResume(e -> {
                            System.err.println("[AnalyticsEventConsumerService] Recovering from error, skipping this event");
                            return Mono.empty();
                        });
                })
                .onErrorResume(e -> {
                    System.err.println("[AnalyticsEventConsumerService] Error in history consumer: " + e.getMessage());
                    e.printStackTrace();
                    return Flux.empty();
                });
    }

    public Flux<AnalyticsEventEntity> consumeAnalyticsEvents() {
        String streamKey = "analytics-events";

        // Get history of events
        Flux<AnalyticsEventEntity> history = redisTemplate.opsForStream()
                .range(streamKey, Range.unbounded())
                .doOnNext(record -> System.out.println("[AnalyticsEventConsumerService] Processing redis record: " + record.getId()))
                .map(record -> {
                    Map<String, String> stringMap = new HashMap<>();
                    record.getValue().forEach((k, v) -> stringMap.put(k.toString(), v.toString()));
                    return MapRecord.create(record.getStream(), stringMap)
                            .withId(record.getId());
                })
                .flatMap(this::mapRecordToEntity)
                .onErrorResume(e -> {
                    System.err.println("[AnalyticsEventConsumerService] Error processing history: " + e.getMessage());
                    e.printStackTrace();
                    return Flux.empty();
                })
                .doOnComplete(() -> System.out.println("[AnalyticsEventConsumerService] History processing completed"));

        // Listen for new events from latest offset
        Flux<MapRecord<String, String, String>> liveStream = receiver.receive(
                StreamOffset.create(streamKey, ReadOffset.latest())
        );

        Flux<AnalyticsEventEntity> live = liveStream
                .flatMap(this::mapRecordToEntity)
                .onErrorResume(e -> {
                    System.err.println("[AnalyticsEventConsumerService] Error processing live event: " + e.getMessage());
                    return Flux.empty();
                });

        // Combine history and live, then save to database
        return Flux.concat(history, live)
                .flatMap(entity -> repository.save(entity)
                    .doOnSuccess(saved -> System.out.println("[AnalyticsEventConsumerService] Successfully persisted event: " + saved.getId()))
                    .doOnError(error -> System.err.println("[AnalyticsEventConsumerService] Failed to save event: " + error.getMessage() + " - " + error.getClass().getName()))
                    .onErrorResume(e -> {
                        e.printStackTrace();
                        return Mono.empty();
                    })
                );
    }

    private Flux<AnalyticsEventEntity> mapRecordToEntity(MapRecord<String, String, String> record) {
        try {
            Map<String, String> data = record.getValue();
            System.out.println("[AnalyticsEventConsumerService] Mapping record with keys: " + data.keySet());
            
            AnalyticsEventEntity entity = new AnalyticsEventEntity();
            entity.setId(record.getId().toString());
            entity.setProjectId(data.get("projectId"));
            entity.setAccountId(data.get("accountId"));
            entity.setEventType(data.getOrDefault("eventType", "request"));
            entity.setPath(data.get("path"));
            entity.setMethod(data.get("method"));
            entity.setIsNew(true);
            
            if (data.containsKey("statusCode")) {
                try {
                    entity.setStatus(Integer.parseInt(data.get("statusCode")));
                } catch (NumberFormatException e) {
                    System.err.println("[AnalyticsEventConsumerService] Failed to parse statusCode: " + data.get("statusCode"));
                }
            }
            
            if (data.containsKey("durationMs")) {
                try {
                    entity.setDuration(Integer.parseInt(data.get("durationMs")));
                } catch (NumberFormatException e) {
                    System.err.println("[AnalyticsEventConsumerService] Failed to parse durationMs: " + data.get("durationMs"));
                }
            }
            
            if (data.containsKey("bytesIn")) {
                try {
                    entity.setBytesIn(Long.parseLong(data.get("bytesIn")));
                } catch (NumberFormatException e) {
                    System.err.println("[AnalyticsEventConsumerService] Failed to parse bytesIn: " + data.get("bytesIn"));
                }
            }
            
            if (data.containsKey("bytesOut")) {
                try {
                    entity.setBytesOut(Long.parseLong(data.get("bytesOut")));
                } catch (NumberFormatException e) {
                    System.err.println("[AnalyticsEventConsumerService] Failed to parse bytesOut: " + data.get("bytesOut"));
                }
            }
            
            entity.setSource(data.get("source"));
            entity.setIp(data.get("ipAddress"));
            entity.setUserAgent(data.get("userAgent"));
            
            // Parse timestamp from record or use current time
            String timestampStr = data.get("timestamp");
            if (timestampStr != null) {
                try {
                    entity.setTimestamp(OffsetDateTime.parse(timestampStr));
                } catch (Exception e) {
                    System.out.println("[AnalyticsEventConsumerService] Could not parse timestamp, using now: " + timestampStr);
                    entity.setTimestamp(OffsetDateTime.now());
                }
            } else {
                entity.setTimestamp(OffsetDateTime.now());
            }

            System.out.println("[AnalyticsEventConsumerService] Mapped entity: projectId=" + entity.getProjectId() + ", accountId=" + entity.getAccountId());
            return Flux.just(entity);
        } catch (Exception e) {
            System.err.println("[AnalyticsEventConsumerService] Error mapping record: " + e.getMessage());
            e.printStackTrace();
            return Flux.empty();
        }
    }

    public Flux<AnalyticsEventEntity> getProjectAnalytics(String projectId, int daysBack) {
        OffsetDateTime since = OffsetDateTime.now().minusDays(daysBack);
        Flux<AnalyticsEventEntity> ans = repository.findByAccountIdAndTimestampGreaterThanOrderByTimestampDesc(projectId, since);

        ans.doOnNext(entity -> {
            System.out.println("[AnalyticsEventConsumerService] " + entity.toString());
        });

        return repository.findByProjectIdAndTimestampGreaterThanOrderByTimestampDesc(projectId, since);
    }

    public Flux<AnalyticsEventEntity> getAccountAnalytics(String accountId, int daysBack) {
        OffsetDateTime since = OffsetDateTime.now().minusDays(daysBack);
        System.out.println(accountId);
        Flux<AnalyticsEventEntity> ans = repository.findByAccountIdAndTimestampGreaterThanOrderByTimestampDesc(accountId, since);

        ans.doOnNext(entity -> {
            System.out.println("[AnalyticsEventConsumerService] " + entity.toString());
        });

        return repository.findByAccountIdAndTimestampGreaterThanOrderByTimestampDesc(accountId, since);
    }
}
