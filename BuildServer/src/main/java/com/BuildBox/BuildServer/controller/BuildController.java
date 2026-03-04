package com.BuildBox.BuildServer.controller;

import com.BuildBox.BuildServer.aws.EcsService;
import com.BuildBox.BuildServer.aws.CloudWatchLogsService;
import com.BuildBox.BuildServer.dto.BuildRequest;
import com.BuildBox.BuildServer.dto.BuildResponse;
import com.BuildBox.BuildServer.model.Project;
import com.BuildBox.BuildServer.model.TaskInfo;
import com.BuildBox.BuildServer.service.AsyncBuildExecutor;
import com.BuildBox.BuildServer.service.RoutingBackend;
import com.BuildBox.BuildServer.service.TaskRegistry;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/builds")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class BuildController {

    @Value("${ecs.cluster.name:buildserver-cluster-1}")
    private String cluster;

    private final AsyncBuildExecutor executor;
    private final TaskRegistry taskRegistry;
    private final EcsService ecsService;
    private final RoutingBackend routingBackend;
    private final CloudWatchLogsService logsService;

    private final com.BuildBox.BuildServer.repository.ProjectRepository projectRepository;

    public BuildController(AsyncBuildExecutor executor,
            TaskRegistry taskRegistry,
            EcsService ecsService,
            RoutingBackend routingBackend,
            CloudWatchLogsService logsService,
            com.BuildBox.BuildServer.repository.ProjectRepository projectRepository) {
        this.executor = executor;
        this.taskRegistry = taskRegistry;
        this.ecsService = ecsService;
        this.routingBackend = routingBackend;
        this.logsService = logsService;
        this.projectRepository = projectRepository;
    }

    @PostMapping
    public BuildResponse triggerBuild(@Valid @RequestBody BuildRequest request) {
        executor.startBuild(
                request.getProjectId(),
                request.getRuntime(),
                request.getDeploymentId(),
                request.getBasePath());

        return new BuildResponse(
                request.getProjectId(),
                "BUILD_STARTED");
    }

    @GetMapping("/tasks/{projectId}")
    public ResponseEntity<TaskInfo> getTaskStatus(@PathVariable String projectId) {
        TaskInfo task = taskRegistry.getTask(projectId);
        if (task == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(task);
    }

    @GetMapping("/tasks/{projectId}/logs")
    public ResponseEntity<List<String>> getTaskLogs(@PathVariable String projectId) {

//        System.out.println(projectId);
//
//        Optional<Project> project = projectRepository.findBySlug(projectId);
//
//        Long id=null;
//        if(project.isPresent()) {
//            id = project.get().getId();
//        }else{
//            return ResponseEntity.notFound().build();
//        }
//
//        System.out.println(id);

//        TaskInfo task = taskRegistry.getTask(id.toString());

        TaskInfo task = taskRegistry.getTask(projectId);

        if (task == null) {
            return ResponseEntity.notFound().build();
        }

        String logGroupName = task.runtime().contains("node") ? "/ecs/user-node-app" : "/ecs/user-python-app";

        System.out.println(logGroupName);

        // Stream prefix is projectId
        String logStreamName = "user-" + task.runtime() + "-app/" + projectId + "/" + task.taskArn().split("/")[2];

        System.out.println(logStreamName);

        return ResponseEntity.ok(logsService.getLogs(logGroupName, logStreamName));
    }

    @DeleteMapping("/tasks/{projectId}")
    public ResponseEntity<Map<String, String>> stopTask(@PathVariable String projectId) {
        TaskInfo task = taskRegistry.getTask(projectId);

        if (task == null) {
            return ResponseEntity.notFound().build();
        }

        System.out.println("🛑 Stopping task for project: " + projectId);

        ecsService.stopTask(cluster, task.taskArn(), "Stopped via API");
        routingBackend.removeRoute(projectId);
        
        // Note: EventBridge will capture the STOPPED event and update lifecycle
        
        taskRegistry.removeTask(projectId);

        return ResponseEntity.ok(Map.of(
                "projectId", projectId,
                "status", "STOPPED",
                "message", "Task stopped, port closed, registry cleaned"));
    }

    @PostMapping("/test-local")
    public BuildResponse triggerLocalBuild(@Valid @RequestBody BuildRequest request) {
        if (projectRepository.findBySlug(request.getProjectId()).isEmpty()) {
            com.BuildBox.BuildServer.model.Project p = new com.BuildBox.BuildServer.model.Project();
            p.setSlug(request.getProjectId());
            p.setName("Test Local Project");
            projectRepository.save(p);
            System.out.println("✅ Seeded test project: " + request.getProjectId());
        }

        executor.startBuildLocal(
                request.getProjectId(),
                request.getRuntime(),
                request.getDeploymentId(),
                request.getBasePath());

        return new BuildResponse(
                request.getProjectId(),
                "LOCAL_BUILD_STARTED");
    }
}
