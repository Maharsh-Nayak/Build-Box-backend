package com.BuildBox.BuildServer.dto;

public class BuildResponse {

    private String projectId;
    private String status;

    public BuildResponse(String projectId, String status) {
        this.projectId = projectId;
        this.status = status;
    }

    public String getProjectId() {
        return projectId;
    }

    public String getStatus() {
        return status;
    }
}
