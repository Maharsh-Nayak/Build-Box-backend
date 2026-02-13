package com.BuildBox.BuildServer.controller;

import com.BuildBox.BuildServer.model.TaskInfo;
import com.BuildBox.BuildServer.service.AsyncBuildExecutor;
import com.BuildBox.BuildServer.service.TaskRegistry;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/internal/apps")
public class InternalAppController {

    private final TaskRegistry taskRegistry;
    private final AsyncBuildExecutor executor;

    public InternalAppController(TaskRegistry taskRegistry, AsyncBuildExecutor executor) {
        this.taskRegistry = taskRegistry;
        this.executor = executor;
    }

    @GetMapping("/{user}/{project}/status")
    public ResponseEntity<Map<String, Object>> getStatus(@PathVariable String user, @PathVariable String project) {
        // Note: user param is part of the contract but currently project IDs are unique
        // globally in our demo
        TaskInfo task = taskRegistry.getTask(project);

        if (task != null && "RUNNING".equals(task.status())) {
            return ResponseEntity.ok(Map.of(
                    "state", "RUNNING",
                    "host", task.host(),
                    "port", task.hostPort()));
        } else {
            return ResponseEntity.ok(Map.of("state", "STOPPED"));
        }
    }

    @PostMapping("/{user}/{project}/start")
    public ResponseEntity<Map<String, String>> startApp(@PathVariable String user, @PathVariable String project) {
        // Infer runtime logic
        String runtime = (project.contains("python") || project.contains("flask")) ? "python" : "node";

        // Trigger local build/start sequence
        executor.startBuildLocal(project, runtime);

        return ResponseEntity.accepted().body(Map.of("status", "STARTING"));
    }
}
