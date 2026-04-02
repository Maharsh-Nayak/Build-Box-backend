package com.buildbox_backend.dto;

import java.util.Map;

public class GitCloneRequest {

    private String link;
    private String projectName;
    private String userId;
    private String frontendDirectory;
    private String backendDirectory;
    Map<String, String> frontendEnvVars;
    Map<String, String> backendEnvVars;


    public GitCloneRequest(String link, String projectName, String userId, String frontendDirectory, String backendDirectory, Map<String, String> customEnvs, Map<String, String> backendEnvVariables) {
        this.link = link;
        this.projectName = projectName;
        this.userId = userId;
        this.frontendDirectory = frontendDirectory;
        this.backendDirectory = backendDirectory;
        this.frontendEnvVars = customEnvs;
        this.backendEnvVars = backendEnvVariables;
    }

    public Map<String, String> getBackendEnvVars() {
        return backendEnvVars;
    }

    public void setBackendEnvVars(Map<String, String> backendEnvVars) {
        this.backendEnvVars = backendEnvVars;
    }

    public Map<String, String> getFrontendEnvVars() {
        return frontendEnvVars;
    }

    public void setFrontendEnvVars(Map<String, String> frontendEnvVars) {
        this.frontendEnvVars = frontendEnvVars;
    }

    public GitCloneRequest() {}

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getProjectName() {return projectName;}

    public void setProjectName(String projectName) {this.projectName = projectName;}

    public String getUserId() {return userId;}
    public void setUserId(String userId) {this.userId = userId;}

    public String getFrontendDirectory() {return frontendDirectory;}

    public void setFrontendDirectory(String frontendDirectory) {this.frontendDirectory = frontendDirectory;}

    public String getBackendDirectory() {return backendDirectory;}

    public void setBackendDirectory(String backendDirectory) {this.backendDirectory = backendDirectory;}
}
