package com.BuildBox.BuildServer.controller;

import com.BuildBox.BuildServer.model.Project;
import com.BuildBox.BuildServer.model.User;
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
        // Look up project metadata from DB for correct runtime/basePath
//        Optional<Project> projectOpt = projectRepository.findBySlug(project);
        System.out.println(project);
        Optional<Project> projectOpt = projectRepository.findByName(project);

        String runtime = (project.contains("python") || project.contains("flask")) ? "python" : "node";
        String basePath = null;

        if (projectOpt.isPresent()) {
            System.out.println(projectOpt.get());
            basePath = projectOpt.get().getBasePath();
            // Optional: runtime could also be stored in DB, but slug-based inference is
            // okay for now
        }

        System.out.println("DEBUG: Cold start for " + project + " | basePath: " + basePath + " | runtime: " + runtime);

        // Trigger local build/start sequence with correct basePath
//        executor.startBuildLocal(project, runtime, null, basePath, null);
        executor.startBuild(project, runtime, null, basePath, null);

        return ResponseEntity.accepted().body(Map.of("status", "STARTING"));
    }
}
