package com.BuildBox.BuildServer.repository;

import com.BuildBox.BuildServer.model.BuildLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BuildLogRepository extends JpaRepository<BuildLog, Long> {

    List<BuildLog> findByDeploymentIdOrderByTimestampAsc(Long deploymentId);
}
