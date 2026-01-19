package com.buildbox_backend.controller;


import com.buildbox_backend.dto.GitCloneRequest;
import com.buildbox_backend.service.ECSService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/deployProject")
public class DeployControllerV2 {

    private ECSService ecsService;

    @Autowired
    public DeployControllerV2(ECSService ecsService) {
        this.ecsService = ecsService;
    }

    @PostMapping("/v2")
    public ResponseEntity<Map<String, String >> deployV2(@RequestBody GitCloneRequest request) {
        System.out.println("Starting deployment for: " + request.getLink());

        System.out.println(request.getUserId());

        Map<String, String> Ids = ecsService.startBuild(request.getLink(), request.getProjectName(), request.getUserId(), request.getBackendDirectory(), request.getFrontendDirectory());

        String buildId = Ids.get("buildId");
        String taskId = Ids.get("taskId");

        return ResponseEntity.accepted().body(Map.of(
                "message", "Deployment started",
                "taskId", taskId,
                "buildId", buildId
        ));
    }
}
