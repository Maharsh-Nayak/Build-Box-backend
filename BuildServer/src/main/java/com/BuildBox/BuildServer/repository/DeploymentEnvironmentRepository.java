package com.BuildBox.BuildServer.repository;

import com.BuildBox.BuildServer.model.DeploymentEnvironment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeploymentEnvironmentRepository extends JpaRepository<DeploymentEnvironment, Long> {
    
    // Get all env vars for a project and environment type
    List<DeploymentEnvironment> findByProjectIdAndEnvironmentType(
        String projectId,
        DeploymentEnvironment.EnvironmentType environmentType
    );
    
    // Get all env vars for a project (both FRONTEND and BACKEND)
    List<DeploymentEnvironment> findByProjectId(String projectId);
    
    // Get specific env var
    Optional<DeploymentEnvironment> findByProjectIdAndEnvironmentTypeAndKey(
        String projectId,
        DeploymentEnvironment.EnvironmentType environmentType,
        String key
    );
    
    // Delete all env vars for a project
    void deleteByProjectId(String projectId);
    
    // Delete specific type for a project
    void deleteByProjectIdAndEnvironmentType(
        String projectId,
        DeploymentEnvironment.EnvironmentType environmentType
    );
}
