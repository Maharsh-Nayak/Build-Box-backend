package com.BuildBox.BuildServer.scheduler;

import com.BuildBox.BuildServer.aws.EcsService;
import com.BuildBox.BuildServer.model.TaskInfo;
import com.BuildBox.BuildServer.service.RoutingBackend;
import com.BuildBox.BuildServer.service.TaskRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
public class TaskCleanupScheduler {

    private final TaskRegistry taskRegistry;
    private final EcsService ecsService;
    private final RoutingBackend routingBackend;

    @Value("${ecs.cluster.name:buildserver-cluster-1}")
    private String cluster;

    @Value("${task.idle.timeout.seconds:120}")
    private long idleTimeoutSeconds;

    public TaskCleanupScheduler(TaskRegistry taskRegistry, EcsService ecsService,
            RoutingBackend routingBackend) {
        this.taskRegistry = taskRegistry;
        this.ecsService = ecsService;
        this.routingBackend = routingBackend;
    }

    @Scheduled(fixedRate = 60000) // Check every minute
    public void cleanupIdleTasks() {
        System.out.println("🧹 Checking for idle tasks...");
        Instant now = Instant.now();

        for (TaskInfo task : taskRegistry.getAllTasks()) {
            if (task.lastActivity() != null) {
                Duration idleDuration = Duration.between(task.lastActivity(), now);
                if (idleDuration.getSeconds() > idleTimeoutSeconds) {
                    System.out.println("⏳ Task for project " + task.projectId() + " has been idle for "
                            + idleDuration.getSeconds() + " seconds. Stopping...");
                    stopTask(task.projectId());
                }
            }
        }
    }

    private void stopTask(String projectId) {
        try {
            TaskInfo task = taskRegistry.getTask(projectId);
            if (task == null)
                return;

            // 1. Stop ECS task
            ecsService.stopTask(cluster, task.taskArn(), "Idle timeout");

            // 2. Remove route
            routingBackend.removeRoute(projectId);

            // 3. Remove from registry
            taskRegistry.removeTask(projectId);

            System.out.println("✅ Stopped idle task: " + projectId);
        } catch (Exception e) {
            System.err.println("❌ Failed to stop idle task for project " + projectId + ": " + e.getMessage());
        }
    }
}
