package com.log_analytics_server.Repository;

import com.log_analytics_server.Model.AnalyticsEventEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;

@Repository
public interface AnalyticsEventRepository extends ReactiveCrudRepository<AnalyticsEventEntity, String> {

    Flux<AnalyticsEventEntity> findByProjectIdOrderByTimestampDesc(String projectId);

    Flux<AnalyticsEventEntity> findByAccountIdOrderByTimestampDesc(String accountId);

    Flux<AnalyticsEventEntity> findByProjectIdAndTimestampGreaterThanOrderByTimestampDesc(String projectId, OffsetDateTime timestamp);

    Flux<AnalyticsEventEntity> findByAccountIdAndTimestampGreaterThanOrderByTimestampDesc(String accountId, OffsetDateTime timestamp);

    Mono<Long> countByProjectIdAndTimestampGreaterThan(String projectId, OffsetDateTime timestamp);

    Mono<Long> countByAccountIdAndTimestampGreaterThan(String accountId, OffsetDateTime timestamp);

    Mono<Long> countByProjectIdAndStatusGreaterThanEqualAndStatusLessThanAndTimestampGreaterThan(String projectId, Integer minStatus, Integer maxStatus, OffsetDateTime timestamp);

    Mono<Long> countByAccountIdAndStatusGreaterThanEqualAndStatusLessThanAndTimestampGreaterThan(String accountId, Integer minStatus, Integer maxStatus, OffsetDateTime timestamp);
}
