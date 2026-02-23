package com.log_analytics_server.Service;

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

import java.util.HashMap;
import java.util.Map;

@Service
public class RuntimeLogService {

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final StreamReceiver<String, MapRecord<String, String, String>> receiver;

    @Autowired
    public RuntimeLogService(
            @Qualifier("reactiveRedisTemplate") ReactiveRedisTemplate<String, String> redisTemplate,
            StreamReceiver<String, MapRecord<String, String, String>> receiver) {
        this.redisTemplate = redisTemplate;
        this.receiver = receiver;
    }

    public Flux<MapRecord<String, String, String>> streamRuntimeLogs(
            String projectId, String lastEventId) {

        String streamKey = "runtime-logs:" + projectId;

        System.out.println(streamKey);

        // History
        Flux<MapRecord<String, String, String>> history = redisTemplate.opsForStream()
                .range(streamKey, Range.unbounded())
                .map(record -> {
                    Map<String, String> stringMap = new HashMap<>();
                    record.getValue().forEach((k, v) -> stringMap.put(k.toString(), v.toString()));
                    return MapRecord.create(record.getStream(), stringMap)
                            .withId(record.getId());
                });

        // Live tail
        ReadOffset offset = lastEventId != null
                ? ReadOffset.from(lastEventId)
                : ReadOffset.latest();
        Flux<MapRecord<String, String, String>> live = receiver.receive(StreamOffset.create(streamKey, offset));

        System.out.println(live.log());

        return Flux.concat(history, live);
    }
}
