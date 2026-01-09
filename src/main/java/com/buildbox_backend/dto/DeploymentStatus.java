package com.buildbox_backend.dto;

import java.util.List;

public class DeploymentStatus {

    private List<String> logs;
    private String status; // "RUNNING", "SUCCESS", "FAILED"

    public List<String> getLogs() {
        return logs;
    }

    public String getStatus() {
        return status;
    }

    public void setLogs(List<String> logs) {
        this.logs = logs;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "DeploymentStatus{" +
                "logs=" + logs +
                ", status='" + status + '\'' +
                '}';
    }
}
