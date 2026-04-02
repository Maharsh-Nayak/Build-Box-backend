package com.BuildBox.BuildServer.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "deployment_environments")
public class DeploymentEnvironment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String projectId;
    
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private EnvironmentType environmentType;
    
    @Column(nullable = false, name = "key_name")
    private String key;
    
    @Column(nullable = false, name = "key_value")
    private String value;
    
    @Column(nullable = false)
    private Boolean isSecret = false;
    
    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
    
    private Instant updatedAt = Instant.now();
    
    private String createdBy;
    
    public enum EnvironmentType {
        FRONTEND,
        BACKEND
    }
    
    // Constructors
    public DeploymentEnvironment() {}
    
    public DeploymentEnvironment(String projectId, EnvironmentType environmentType, String key, String value) {
        this.projectId = projectId;
        this.environmentType = environmentType;
        this.key = key;
        this.value = value;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getProjectId() {
        return projectId;
    }
    
    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }
    
    public EnvironmentType getEnvironmentType() {
        return environmentType;
    }
    
    public void setEnvironmentType(EnvironmentType environmentType) {
        this.environmentType = environmentType;
    }
    
    public String getKey() {
        return key;
    }
    
    public void setKey(String key) {
        this.key = key;
    }
    
    public String getValue() {
        return value;
    }
    
    public void setValue(String value) {
        this.value = value;
    }
    
    public Boolean getIsSecret() {
        return isSecret;
    }
    
    public void setIsSecret(Boolean secret) {
        isSecret = secret;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
    
    public Instant getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public String getCreatedBy() {
        return createdBy;
    }
    
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
}
