package com.log_analytics_server.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
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
public class BuildLogsService {

    private ReactiveRedisTemplate<String, String> redisTemplate;

    private StreamReceiver<String, MapRecord<String,String,String>> receiver;

    @Autowired
    public BuildLogsService(@Qualifier("reactiveRedisTemplate") ReactiveRedisTemplate<String, String> redisTemplate, StreamReceiver<String, MapRecord<String,String,String>> receiver) {
        this.redisTemplate = redisTemplate;
        this.receiver = receiver;
    }
    
    public Flux<MapRecord<String,String,String>> streamLogs(
            String buildId,
            String lastEventId
    ) {

        String normalizedBuildId = buildId != null && buildId.startsWith("logs:")
                ? buildId.substring("logs:".length())
                : buildId;
        String streamKey = "logs:" + normalizedBuildId;

        // 1️⃣ History
        Flux<MapRecord<String,String,String>> history =
                redisTemplate.opsForStream()
                        .range(streamKey, Range.unbounded()).map(record -> {
                            // Convert the internal Map values to Strings
                            Map<String, String> stringMap = new HashMap<>();
                            record.getValue().forEach((k, v) ->
                                    stringMap.put(k.toString(), v.toString())
                            );

                            // Create a new MapRecord with the new types
                            return MapRecord.create(record.getStream(), stringMap)
                                    .withId(record.getId());
                        });

        // 2️⃣ Live tail
        ReadOffset offset = lastEventId != null
                ? ReadOffset.from(lastEventId)
                : ReadOffset.latest();

        Flux<MapRecord<String,String,String>> live =
                receiver.receive(StreamOffset.create(streamKey, offset));

        // 3️⃣ Combine
        return Flux.concat(history, live);
    }
}
