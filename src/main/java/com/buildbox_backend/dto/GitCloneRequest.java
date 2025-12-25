package com.buildbox_backend.dto;

public class GitCloneRequest {

    private String link;
    private String projectName;
    public GitCloneRequest(String link, String projectName) {
        this.link = link;
        this.projectName = projectName;
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
}
