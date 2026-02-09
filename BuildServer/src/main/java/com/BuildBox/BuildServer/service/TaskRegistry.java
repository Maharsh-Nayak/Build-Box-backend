package com.BuildBox.BuildServer.service;

import com.BuildBox.BuildServer.model.TaskInfo;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TaskRegistry {

    private final Map<String, TaskInfo> activeTasks = new ConcurrentHashMap<>();

    public void register(String projectId, String taskArn, String runtime, String host, int port) {
        TaskInfo task = new TaskInfo(
            projectId,
            taskArn,
            runtime,
            host,
            port,
            "RUNNING",
            Instant.now()
        );
        activeTasks.put(projectId, task);
        System.out.println("📝 Registered task for project: " + projectId + " at " + host + ":" + port);
    }

    public TaskInfo getTask(String projectId) {
        return activeTasks.get(projectId);
    }

    public Collection<TaskInfo> getAllTasks() {
        return activeTasks.values();
    }

    public void removeTask(String projectId) {
        activeTasks.remove(projectId);
        System.out.println("🗑️ Removed task for project: " + projectId);
    }
}
