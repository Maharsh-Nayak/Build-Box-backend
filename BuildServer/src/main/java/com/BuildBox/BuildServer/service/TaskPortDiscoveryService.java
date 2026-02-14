package com.BuildBox.BuildServer.service;

import com.BuildBox.BuildServer.aws.EcsService;
import com.BuildBox.BuildServer.model.TaskInfo;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class TaskPortDiscoveryService {

    private final EcsService ecsService;
    private final TaskRegistry taskRegistry;
    private final RoutingBackend routingBackend;

    public TaskPortDiscoveryService(EcsService ecsService,
            TaskRegistry taskRegistry,
            RoutingBackend routingBackend) {
        this.ecsService = ecsService;
        this.taskRegistry = taskRegistry;
        this.routingBackend = routingBackend;
    }

    /**
     * Polls ECS for the task status until it is RUNNING, then:
     * 1. Discovers the dynamically assigned port
     * 2. Adds route via active backend (Nginx or ALB)
     * 3. Registers the task in the registry
     */
    public TaskInfo discoverAndRegister(String cluster, String taskArn, String projectId, String runtime,
            String containerName) {
        System.out.println("🔍 Waiting for task to start... (Task ARN: " + taskArn + ")");

        int attempts = 0;
        int maxAttempts = 90; // 90 * 2s = 3 minutes timeout

        while (attempts < maxAttempts) {
            try {
                String status = ecsService.getTaskStatus(cluster, taskArn);
                System.out.println("   [Attempt " + (attempts + 1) + "] Task Status: " + status);

                if ("RUNNING".equals(status)) {
                    System.out.println("✅ Task is RUNNING. Discovering port...");

                    Integer port = ecsService.getAssignedPort(cluster, taskArn, containerName);

                    if (port != null) {
                        String host = ecsService.getContainerInstanceIp(cluster, taskArn);
                        System.out.println("✅ Port discovered: " + port);
                        System.out.println("✅ Host discovered: " + host);

                        // Provision route via active backend (Nginx or ALB)
                        routingBackend.addRoute(projectId, host, port);

                        // Register in task registry
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
