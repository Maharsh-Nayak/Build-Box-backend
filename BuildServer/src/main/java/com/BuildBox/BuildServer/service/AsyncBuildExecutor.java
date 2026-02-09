package com.BuildBox.BuildServer.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AsyncBuildExecutor {

    private final BuildService buildService;

    public AsyncBuildExecutor(BuildService buildService) {
        this.buildService = buildService;
    }

    @Async
    public void startBuild(String projectId, String runtime) {
        try {
            buildService.buildAndRun(projectId, runtime);
        } catch (Exception e) {
            logError(projectId, e);
        }
    }

    @Async
    public void startBuildLocal(String projectId, String runtime) {
        try {
            buildService.buildAndRunLocal(projectId, runtime);
        } catch (Exception e) {
            logError(projectId, e);
        }
    }

    private void logError(String projectId, Exception e) {
        e.printStackTrace();
        try {
            java.nio.file.Files.writeString(
                java.nio.file.Path.of("build_error_" + projectId + ".log"),
                e.toString() + "\n" + java.util.Arrays.toString(e.getStackTrace()),
                java.nio.file.StandardOpenOption.CREATE,
                java.nio.file.StandardOpenOption.APPEND
            );
        } catch (java.io.IOException ioException) {
            ioException.printStackTrace();
        }
    }
}