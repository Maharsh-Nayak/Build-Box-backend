package com.BuildBox.BuildServer.controller;

import com.BuildBox.BuildServer.aws.EcsService;
import com.BuildBox.BuildServer.aws.SecurityGroupService;
import com.BuildBox.BuildServer.dto.BuildRequest;
import com.BuildBox.BuildServer.dto.BuildResponse;
import com.BuildBox.BuildServer.model.TaskInfo;
import com.BuildBox.BuildServer.service.AsyncBuildExecutor;
import com.BuildBox.BuildServer.service.NginxRoutingService;
import com.BuildBox.BuildServer.service.TaskRegistry;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/builds")
public class BuildController {

    @Value("${ecs.cluster.name:buildserver-cluster-1}")
    private String cluster;

    private final AsyncBuildExecutor executor;
    private final TaskRegistry taskRegistry;
    private final EcsService ecsService;
    private final SecurityGroupService securityGroupService;
    private final NginxRoutingService nginxRoutingService;

    public BuildController(AsyncBuildExecutor executor,
            TaskRegistry taskRegistry,
            EcsService ecsService,
            SecurityGroupService securityGroupService,
            NginxRoutingService nginxRoutingService) {
        this.executor = executor;
        this.taskRegistry = taskRegistry;
        this.ecsService = ecsService;
        this.securityGroupService = securityGroupService;
        this.nginxRoutingService = nginxRoutingService;
    }

    @PostMapping
    public BuildResponse triggerBuild(@Valid @RequestBody BuildRequest request) {
        executor.startBuild(
                request.getProjectId(),
                request.getRuntime());

        return new BuildResponse(
                request.getProjectId(),
                "BUILD_STARTED");
    }

    @GetMapping("/tasks/{projectId}")
    public TaskInfo getTaskStatus(@PathVariable String projectId) {
        TaskInfo task = taskRegistry.getTask(projectId);
        if (task == null) {
            throw new RuntimeException("Task not found for project: " + projectId);
        }
        return task;
    }

    /**
     * Stop a running task and clean up resources.
     * - Stops the ECS task
     * - Closes the security group port
     * - Removes from task registry
     */
    @DeleteMapping("/tasks/{projectId}")
    public ResponseEntity<Map<String, String>> stopTask(@PathVariable String projectId) {
        TaskInfo task = taskRegistry.getTask(projectId);

        if (task == null) {
            return ResponseEntity.notFound().build();
        }

        System.out.println("🛑 Stopping task for project: " + projectId);

        // 1. Stop the ECS task
        ecsService.stopTask(cluster, task.taskArn(), "Stopped via API");

        // 2. Close the security group port
        securityGroupService.closePort(task.hostPort());

        // 3. Remove Nginx route
        nginxRoutingService.removeRoute(projectId);

        // 4. Remove from registry
        taskRegistry.removeTask(projectId);

        System.out.println("✅ Task stopped and cleaned up: " + projectId);

        return ResponseEntity.ok(Map.of(
                "projectId", projectId,
                "status", "STOPPED",
                "message", "Task stopped, port closed, registry cleaned"));
    }

    /**
     * Test endpoint - uses LOCAL code instead of S3.
     */
    @PostMapping("/test-local")
    public BuildResponse triggerLocalBuild(@Valid @RequestBody BuildRequest request) {
        executor.startBuildLocal(
                request.getProjectId(),
                request.getRuntime());

        return new BuildResponse(
                request.getProjectId(),
                "LOCAL_BUILD_STARTED");
    }
}
