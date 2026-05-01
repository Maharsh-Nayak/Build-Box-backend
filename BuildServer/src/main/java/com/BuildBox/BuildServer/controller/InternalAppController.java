package com.BuildBox.BuildServer.controller;

import com.BuildBox.BuildServer.model.Project;
import com.BuildBox.BuildServer.repository.ProjectRepository;
import com.BuildBox.BuildServer.service.AsyncBuildExecutor;
import com.BuildBox.BuildServer.service.TaskRegistry;
import com.BuildBox.BuildServer.model.TaskInfo;
import com.BuildBox.BuildServer.service.RoutingBackend;
import com.BuildBox.BuildServer.dto.RoutingDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/internal/apps")
public class InternalAppController {

    private final TaskRegistry taskRegistry;
    private final AsyncBuildExecutor executor;
    private final ProjectRepository projectRepository;
    private final RoutingBackend routingBackend;

    public InternalAppController(TaskRegistry taskRegistry, AsyncBuildExecutor executor,
            ProjectRepository projectRepository, RoutingBackend routingBackend) {
        this.taskRegistry = taskRegistry;
        this.executor = executor;
        this.projectRepository = projectRepository;
        this.routingBackend = routingBackend;
    }

    @GetMapping("/{user}/{project}/status")
    public ResponseEntity<Map<String, Object>> getStatus(@PathVariable String user, @PathVariable String project) {
        // Note: user param is part of the contract but currently project IDs are unique
        // globally in our demo
        TaskInfo task = taskRegistry.getTask(project);

        if (task != null && "RUNNING".equals(task.status())) {
            // Reset idle timer on every status check (proxy calls this for each request)
            taskRegistry.updateActivity(project);

            RoutingDetails routing = routingBackend.getRoutingDetails(project);

            // For backward compatibility while the proxy is being updated, we can still
            // include host/port
            return ResponseEntity.ok(Map.of(
                    "state", "RUNNING",
                    "host", task.host(),
                    "port", task.hostPort(),
                    "routing", routing != null ? routing : Map.of()));
        } else {
            return ResponseEntity.ok(Map.of("state", "STOPPED"));
        }
    }

    @PostMapping("/{user}/{project}/start")
    public ResponseEntity<Map<String, String>> startApp(@PathVariable String user, @PathVariable String project) {
        TaskInfo task = taskRegistry.getTask(project);

        if (task != null && "RUNNING".equals(task.status())) {
            return ResponseEntity.ok(Map.of("status", "ALREADY_RUNNING"));
        }

        Optional<Project> projectOpt = projectRepository.findBySlug(project);
        if (projectOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "FAILED",
                    "message", "Project not found for slug: " + project));
        }

        Project projectMetadata = projectOpt.get();
        String runtime = (project.contains("python") || project.contains("flask")) ? "python" : "node";
        String basePath = projectMetadata.getBasePath();
        String repoUrl = projectMetadata.getRepoUrl();

        if (repoUrl == null || repoUrl.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "FAILED",
                    "message", "Project has no repoUrl; cannot cold start"));
        }

        System.out.println("DEBUG: Cold start for " + project + " | basePath: " + basePath + " | runtime: " + runtime);

        executor.startBuild(project, runtime, null, basePath, repoUrl);

        return ResponseEntity.accepted().body(Map.of("status", "STARTING"));
    }
}
