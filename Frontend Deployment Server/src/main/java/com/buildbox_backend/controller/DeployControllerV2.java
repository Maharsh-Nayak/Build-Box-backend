package com.buildbox_backend.controller;

import com.buildbox_backend.model.Project;
import com.buildbox_backend.model.Deployment;
import com.buildbox_backend.model.User;
import com.buildbox_backend.dto.GitCloneRequest;
import com.buildbox_backend.repository.ProjectRepository;
import com.buildbox_backend.repository.UserRepository;
import com.buildbox_backend.service.DeploymentService;
import com.buildbox_backend.service.ECSService;
import com.buildbox_backend.service.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/deployProject")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class DeployControllerV2 {

    private ECSService ecsService;
    private ProjectService projectService;
    private UserRepository userRepository;
    private DeploymentService deploymentService;
    private ProjectRepository projectRepository;

    @Autowired
    public DeployControllerV2(ECSService ecsService, ProjectService projectService, 
                             UserRepository userRepository, DeploymentService deploymentService,
                             ProjectRepository projectRepository) {
        this.ecsService = ecsService;
        this.projectService = projectService;
        this.userRepository = userRepository;
        this.deploymentService = deploymentService;
        this.projectRepository = projectRepository;
    }

    @PostMapping("/v2")
    public ResponseEntity<Map<String, String>> deployV2(@RequestBody GitCloneRequest request) {
        System.out.println("Starting deployment for: " + request.getLink());

        Optional<User> u = userRepository.findById(Long.valueOf(request.getUserId()));
        if (u.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("error", "User not found"));
        }

        // 1. Find or Create Project
        String slug = request.getProjectName().toLowerCase().replaceAll("[^a-z0-9]+", "-");
        Project project = projectRepository.findBySlug(slug)
                .orElseGet(() -> projectService.createProject(
                        request.getProjectName(), 
                        request.getLink(), 
                        request.getFrontendDirectory(), 
                        u.get()
                ));

        // 2. Start ECS Build
        Map<String, String> ids = ecsService.startBuild(request.getLink(), request.getProjectName(),
                request.getUserId(), request.getBackendDirectory(), request.getFrontendDirectory());

        String buildId = ids.get("buildId");
        String taskId = ids.get("taskId");

        // 3. Create Deployment Record
        Deployment deployment = deploymentService.createDeploymentRecord(project, "main", taskId, "V2 ECS Deployment");

        return ResponseEntity.accepted().body(Map.of(
                "message", "Deployment started",
                "taskId", taskId,
                "buildId", buildId,
                "deploymentId", String.valueOf(deployment.getId())));
    }
}
