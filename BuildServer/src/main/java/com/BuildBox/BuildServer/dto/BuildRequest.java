package com.BuildBox.BuildServer.dto;

import jakarta.validation.constraints.NotBlank;

public class BuildRequest {

    @NotBlank
    private String projectId;

    @NotBlank
    private String runtime;

    public String getProjectId() {
        return projectId;
    }

    public String getRuntime() {
        return runtime;
    }
}
