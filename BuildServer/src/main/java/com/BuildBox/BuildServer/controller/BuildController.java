package com.BuildBox.BuildServer.controller;

import com.BuildBox.BuildServer.dto.BuildRequest;
import com.BuildBox.BuildServer.dto.BuildResponse;
import com.BuildBox.BuildServer.service.AsyncBuildExecutor;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/builds")
public class BuildController {

    private final AsyncBuildExecutor executor;
    private final com.BuildBox.BuildServer.service.TaskRegistry taskRegistry;

    public BuildController(AsyncBuildExecutor executor, com.BuildBox.BuildServer.service.TaskRegistry taskRegistry) {
        this.executor = executor;
        this.taskRegistry = taskRegistry;
    }

    @PostMapping
    public BuildResponse triggerBuild(@Valid @RequestBody BuildRequest request) {

        executor.startBuild(
                request.getProjectId(),
                request.getRuntime()
        );

        return new BuildResponse(
                request.getProjectId(),
                "BUILD_STARTED"
        );
    }

    @GetMapping("/tasks/{projectId}")
    public com.BuildBox.BuildServer.model.TaskInfo getTaskStatus(@PathVariable String projectId) {
        return taskRegistry.getTask(projectId);
    }

    /**
     * Test endpoint - uses LOCAL code instead of S3.
     * For testing Docker build → ECR push → ECS deploy flow.
     */
    @PostMapping("/test-local")
    public BuildResponse triggerLocalBuild(@Valid @RequestBody BuildRequest request) {

        executor.startBuildLocal(
                request.getProjectId(),
                request.getRuntime()
        );

        return new BuildResponse(
                request.getProjectId(),
                "LOCAL_BUILD_STARTED"
        );
    }
}
