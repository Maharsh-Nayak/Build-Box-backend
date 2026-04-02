package com.BuildBox.BuildServer.service;

import com.BuildBox.BuildServer.model.DeploymentEnvironment;
import com.BuildBox.BuildServer.repository.DeploymentEnvironmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DeploymentEnvironmentService {
    
    private final DeploymentEnvironmentRepository repository;
    
    public DeploymentEnvironmentService(DeploymentEnvironmentRepository repository) {
        this.repository = repository;
    }
    
    /**
     * Save or update environment variable for a project
     */
    @Transactional
    public DeploymentEnvironment saveEnvironmentVariable(
        String projectId,
        DeploymentEnvironment.EnvironmentType type,
        String key,
        String value,
        Boolean isSecret,
        String createdBy
    ) {
        // Check if already exists
        var existing = repository.findByProjectIdAndEnvironmentTypeAndKey(projectId, type, key);
        
        if (existing.isPresent()) {
            // Update existing
            DeploymentEnvironment env = existing.get();
            env.setValue(value);
            env.setIsSecret(isSecret != null ? isSecret : false);
            env.setUpdatedAt(Instant.now());
            return repository.save(env);
        } else {
            // Create new
            DeploymentEnvironment env = new DeploymentEnvironment(projectId, type, key, value);
            env.setIsSecret(isSecret != null ? isSecret : false);
            env.setCreatedBy(createdBy);
            env.setCreatedAt(Instant.now());
            env.setUpdatedAt(Instant.now());
            return repository.save(env);
        }
    }
    
    /**
     * Get all env vars for a specific environment type
     */
    public List<DeploymentEnvironment> getEnvironmentVariables(
        String projectId,
        DeploymentEnvironment.EnvironmentType type
    ) {
        return repository.findByProjectIdAndEnvironmentType(projectId, type);
    }
    
    /**
     * Get all env vars as Map for ECS task definition
     */
    public Map<String, String> getEnvironmentVariablesAsMap(
        String projectId,
        DeploymentEnvironment.EnvironmentType type
    ) {
        return repository.findByProjectIdAndEnvironmentType(projectId, type)
            .stream()
            .collect(Collectors.toMap(DeploymentEnvironment::getKey, DeploymentEnvironment::getValue));
    }
    
    /**
     * Delete specific env var
     */
    @Transactional
    public void deleteEnvironmentVariable(
        String projectId,
        DeploymentEnvironment.EnvironmentType type,
        String key
    ) {
        repository.findByProjectIdAndEnvironmentTypeAndKey(projectId, type, key)
            .ifPresent(repository::delete);
    }
    
    /**
     * Delete all env vars for a project environment type
     */
    @Transactional
    public void deleteAllEnvironmentVariables(
        String projectId,
        DeploymentEnvironment.EnvironmentType type
    ) {
        repository.deleteByProjectIdAndEnvironmentType(projectId, type);
    }
    
    /**
     * Delete all env vars for a project
     */
    @Transactional
    public void deleteAllEnvironmentVariablesForProject(String projectId) {
        repository.deleteByProjectId(projectId);
    }
}
