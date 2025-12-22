package com.buildbox_backend.dto;

public class GitCloneRequest {

    private String link;
    public GitCloneRequest(String link) {
        this.link = link;
    }

    public GitCloneRequest() {}

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }
}
