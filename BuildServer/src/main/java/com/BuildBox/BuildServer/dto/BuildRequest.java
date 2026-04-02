package com.BuildBox.BuildServer.dto;

import jakarta.validation.constraints.NotBlank;

public class BuildRequest {

    @NotBlank
    private String projectId;

    @NotBlank
    private String runtime;

    private Long deploymentId; // Optional: links build to a deployment record
    private String basePath; // Optional: sub-directory within the project (e.g. "Backend")
    private String repoUrl; // Optional: git repository URL to clone from

    public String getProjectId() {
        return projectId;
    }

    public String getRuntime() {
        return runtime;
    }

    public Long getDeploymentId() {
        return deploymentId;
    }

    public String getBasePath() {
        return basePath;
    }

    public String getRepoUrl() {
        return repoUrl;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public void setRuntime(String runtime) {
        this.runtime = runtime;
    }

    public void setDeploymentId(Long deploymentId) {
        this.deploymentId = deploymentId;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    public void setRepoUrl(String repoUrl) {
        this.repoUrl = repoUrl;
    }
}
