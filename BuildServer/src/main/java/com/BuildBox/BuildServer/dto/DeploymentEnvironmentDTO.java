package com.BuildBox.BuildServer.dto;

import java.time.Instant;

public class DeploymentEnvironmentDTO {
    
    private Long id;
    private String projectId;
    private String environmentType; // FRONTEND or BACKEND
    private String key;
    private String value;
    private Boolean isSecret;
    private Instant createdAt;
    private Instant updatedAt;
    private String createdBy;
    
    public DeploymentEnvironmentDTO() {}
    
    public DeploymentEnvironmentDTO(String key, String value, Boolean isSecret) {
        this.key = key;
        this.value = value;
        this.isSecret = isSecret != null ? isSecret : false;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    
    public String getEnvironmentType() { return environmentType; }
    public void setEnvironmentType(String environmentType) { this.environmentType = environmentType; }
    
    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
    
    public Boolean getIsSecret() { return isSecret; }
    public void setIsSecret(Boolean secret) { isSecret = secret; }
    
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
}
