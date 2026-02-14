package com.buildbox_backend.repository;

import com.buildbox_backend.model.Deployment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeploymentRepository extends JpaRepository<Deployment, Long> {

    List<Deployment> findByProjectIdOrderByCreatedAtDesc(Long projectId);

    List<Deployment> findByProjectUserIdOrderByCreatedAtDesc(Long userId);

    Optional<Deployment> findTopByProjectIdOrderByVersionDesc(Long projectId);
}
