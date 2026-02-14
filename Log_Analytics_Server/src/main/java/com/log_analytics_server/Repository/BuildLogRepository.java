package com.log_analytics_server.Repository;

import com.log_analytics_server.Model.BuildLogEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface BuildLogRepository extends ReactiveCrudRepository<BuildLogEntity, Long> {

    Flux<BuildLogEntity> findByDeploymentIdOrderByTimestampAsc(Long deploymentId);
}
