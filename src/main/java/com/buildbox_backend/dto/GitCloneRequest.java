package com.buildbox_backend.dto;

public class GitCloneRequest {

    private String link;
    private String projectName;
    private String userId;
    private String frontendDirectory;
    private String backendDirectory;


    public GitCloneRequest(String link, String projectName, String userId, String frontendDirectory, String backendDirectory) {
        this.link = link;
        this.projectName = projectName;
        this.userId = userId;
        this.frontendDirectory = frontendDirectory;
        this.backendDirectory = backendDirectory;
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
