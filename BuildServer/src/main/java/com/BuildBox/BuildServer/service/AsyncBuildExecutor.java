package com.BuildBox.BuildServer.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.stream.Stream;

@Service
public class AsyncBuildExecutor {

    @Value("${build.base.dir:/tmp/builds}")
    private String BASE_DIR;

    private final BuildService buildService;
    private final DeploymentTrackingService tracker;

    public AsyncBuildExecutor(BuildService buildService, DeploymentTrackingService tracker) {
        this.buildService = buildService;
        this.tracker = tracker;
    }

//    @Async
    public void startBuild(String projectId, String runtime, Long deploymentId, String basePath) {
        try {
            buildService.buildAndRun(projectId, runtime, deploymentId, basePath);
        } catch (Exception e) {
            tracker.log(deploymentId, "❌ Build failed: " + e.getMessage());
            tracker.complete(deploymentId, false);
            logError(projectId, e);
        }
    }

    @Async
    public void startBuildLocal(String projectId, String runtime, Long deploymentId, String basePath) {
        try {
            buildService.buildAndRunLocal(projectId, runtime, deploymentId, basePath);
        } catch (Exception e) {
            tracker.log(deploymentId, "❌ Build failed: " + e.getMessage());
            tracker.complete(deploymentId, false);
            logError(projectId, e);
        }
    }

    // Backwards-compatible overloads
    @Async
    public void startBuild(String projectId, String runtime, Long deploymentId) {
        startBuild(projectId, runtime, deploymentId, null);
    }

    @Async
    public void startBuild(String projectId, String runtime) {
        startBuild(projectId, runtime, null, null);
    }

    @Async
    public void startBuildLocal(String projectId, String runtime, Long deploymentId) {
        startBuildLocal(projectId, runtime, deploymentId, null);
    }

    @Async
    public void startBuildLocal(String projectId, String runtime) {
        startBuildLocal(projectId, runtime, null, null);
    }

    private void logError(String projectId, Exception e) {
        e.printStackTrace();
        try {
            java.nio.file.Files.writeString(
                    java.nio.file.Path.of("build_error_" + projectId + ".log"),
                    e.toString() + "\n" + java.util.Arrays.toString(e.getStackTrace()),
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.APPEND);
        } catch (java.io.IOException ioException) {
            ioException.printStackTrace();
        }
    }
}