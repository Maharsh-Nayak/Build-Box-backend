package com.BuildBox.BuildServer.service;

import com.BuildBox.BuildServer.aws.EcsService;
import com.BuildBox.BuildServer.model.TaskInfo;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class TaskPortDiscoveryService {

    private final EcsService ecsService;
    private final TaskRegistry taskRegistry;

    public TaskPortDiscoveryService(EcsService ecsService, TaskRegistry taskRegistry) {
        this.ecsService = ecsService;
        this.taskRegistry = taskRegistry;
    }

    /**
     * Polls ECS for the task status until it is RUNNING, then discovers the port
     * and registers the task in the registry.
     * This is a blocking operation, so it should be run asynchronously if possible,
     * or the client should expect a delay.
     */
    public TaskInfo discoverAndRegister(String cluster, String taskArn, String projectId, String runtime, String containerName) {
        System.out.println("🔍 Waiting for task to start... (Task ARN: " + taskArn + ")");

        int attempts = 0;
        int maxAttempts = 20; // 20 * 2s = 40 seconds timeout
        
        while (attempts < maxAttempts) {
            try {
                if (ecsService.isTaskRunning(cluster, taskArn)) {
                    System.out.println("✅ Task is RUNNING. Discovering port...");
                    
                    Integer port = ecsService.getAssignedPort(cluster, taskArn, containerName);
                    
                    if (port != null) {
                        String host = ecsService.getContainerInstanceIp(cluster, taskArn);
                        System.out.println("✅ Port discovered: " + port);
                        System.out.println("✅ Host discovered: " + host);
                        
                        taskRegistry.register(projectId, taskArn, runtime, host, port);
                        return taskRegistry.getTask(projectId);
                    }
                }
            } catch (Exception e) {
                // Ignore errors during polling (e.g. task not found yet)
                System.out.println("   ... waiting for task (attempt " + (attempts + 1) + ")");
            }

            try {
                TimeUnit.SECONDS.sleep(2);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while waiting for task", e);
            }
            
            attempts++;
        }

        throw new RuntimeException("Timeout waiting for task to start: " + taskArn);
    }
}
