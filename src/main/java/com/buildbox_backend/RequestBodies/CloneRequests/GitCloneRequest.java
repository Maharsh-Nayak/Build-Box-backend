package com.buildbox_backend.RequestBodies.CloneRequests;

public class GitCloneRequest {

    private String link;
    public GitCloneRequest(String link) {
        this.link = link;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }
}
