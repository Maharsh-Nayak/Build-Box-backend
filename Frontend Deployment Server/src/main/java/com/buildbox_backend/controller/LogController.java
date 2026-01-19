package com.buildbox_backend.controller;

import com.buildbox_backend.dto.DeploymentStatus;
import com.buildbox_backend.service.CloudWatchService;
import com.buildbox_backend.service.DeployService;
import com.buildbox_backend.service.ECSService;
import com.buildbox_backend.service.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;

@RestController
@RequestMapping("/deploymentLogs")
public class LogController {

    private final CloudWatchService cloudWatchService;
    private final ECSService ecsService;

    @Autowired
    public LogController(CloudWatchService cloudWatchService, ECSService ecsService, ProjectService projectService, DeployService deployService) {
        this.cloudWatchService = cloudWatchService;
        this.ecsService = ecsService;
    }

    @GetMapping(value = "/{taskId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<DeploymentStatus> getLogEvents(@PathVariable String taskId) {

        System.out.println("Streaming logs for task: " + taskId);

        String logGroup = "/ecs/BuildBoxDeploy";
        String logStream = "ecs/InitialDeploy/" + taskId;

        return Flux.interval(Duration.ofSeconds(2))
                .map(tick -> {
                    DeploymentStatus dto = new DeploymentStatus();
                    dto.setLogs(cloudWatchService.getLogs(logGroup, logStream));
                    dto.setStatus(ecsService.getTaskStatus(taskId));
                    return dto;
                })
                .doOnNext(dto -> {
                    if("SUCCESS".equals(dto.getStatus())) {

                    }
                })
                .takeUntil(dto -> List.of("SUCCESS", "FAILED").contains(dto.getStatus()));
    }
}
