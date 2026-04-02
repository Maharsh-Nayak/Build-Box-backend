package com.BuildBox.BuildServer.dto;

import java.util.List;

public class EnvironmentVariablesRequest {
    
    private String projectId;
    private String environmentType; // FRONTEND or BACKEND
    private List<DeploymentEnvironmentDTO> variables;
    
    public EnvironmentVariablesRequest() {}
    
    public EnvironmentVariablesRequest(String projectId, String environmentType, List<DeploymentEnvironmentDTO> variables) {
        this.projectId = projectId;
        this.environmentType = environmentType;
        this.variables = variables;
    }
    
    // Getters and Setters
    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    
    public String getEnvironmentType() { return environmentType; }
    public void setEnvironmentType(String environmentType) { this.environmentType = environmentType; }
    
    public List<DeploymentEnvironmentDTO> getVariables() { return variables; }
    public void setVariables(List<DeploymentEnvironmentDTO> variables) { this.variables = variables; }
}
