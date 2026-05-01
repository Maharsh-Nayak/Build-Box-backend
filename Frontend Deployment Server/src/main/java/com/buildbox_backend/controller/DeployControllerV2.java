package com.buildbox_backend.controller;

import com.buildbox_backend.dto.GitCloneRequest;
import com.buildbox_backend.model.User;
import com.buildbox_backend.repository.UserRepository;
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
import org.springframework.jdbc.core.JdbcTemplate;
import com.buildbox_backend.service.DeploymentService;

@RestController
@RequestMapping("/deployProject")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class DeployControllerV2 {

    private ECSService ecsService;
    private ProjectService projectService;
    private UserRepository userRepository;
    private JdbcTemplate jdbcTemplate;
    private DeploymentService deploymentService;

    @Autowired
    public DeployControllerV2(ECSService ecsService, ProjectService projectService, UserRepository userRepository, JdbcTemplate jdbcTemplate, DeploymentService deploymentService) {
        this.ecsService = ecsService;
        this.projectService = projectService;
        this.userRepository = userRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.deploymentService = deploymentService;
    }

    @PostMapping("/v2")
    public ResponseEntity<Map<String, String>> deployV2(@RequestBody GitCloneRequest request) {
        System.out.println("Starting deployment for: " + request.getLink());

        System.out.println(request.getUserId());

        Optional<User> u = userRepository.findById(Long.valueOf(request.getUserId()));
        var createdProject = projectService.createProject(
            request.getProjectName(),
            request.getLink(),
            request.getBackendDirectory(), // Fix: Pass backend directory for BuildServer context
            u.get());

        String slug = createdProject.getSlug();

        // Automatically inject the backend API URL for the frontend build
        Map<String, String> frontendEnvs = request.getFrontendEnvVars();
        if (frontendEnvs == null) {
            frontendEnvs = new java.util.HashMap<>();
        }
        frontendEnvs.put("VITE_API_URL", "https://" + slug + "-api.buildbox.tech");

        Map<String, String> Ids = ecsService.startBuild(request.getLink(), request.getProjectName(),
                request.getUserId(), request.getBackendDirectory(), request.getFrontendDirectory(), frontendEnvs, request.getBackendEnvVars());

        String buildId = Ids.get("buildId");
        String taskId = Ids.get("taskId");

        // Save env vars to deployment_environments so BuildServer injects them at container start
        if (request.getBackendEnvVars() != null) {


            for(String key : request.getBackendEnvVars().keySet()) {
                String value = request.getBackendEnvVars().get(key);
                String name = key;

                System.out.println("BACKEND_ENV_VARS: ");
                System.out.println(name + " = " + value);

                String environment = "BACKEND";
                if (name != null && !name.isBlank() && value != null) {
                    jdbcTemplate.update(
                            "INSERT INTO deployment_environments (project_id, environment_type, key_name, key_value, is_secret, created_at, updated_at) " +
                                    "VALUES (?, ?, ?, ?, false, NOW(), NOW()) " +
                                    "ON CONFLICT (project_id, environment_type, key_name) DO UPDATE SET key_value = EXCLUDED.key_value, updated_at = NOW()",
                            slug, environment, name, value
                    );
                }
            }
        }

        // Trigger the backend deployment via BuildServer
        deploymentService.triggerDeployment(createdProject, "main", "initial-commit", "Initial deployment from UI");

        return ResponseEntity.accepted().body(Map.of(
                "message", "Deployment started",
                "taskId", taskId,
            "buildId", buildId,
            "projectSlug", slug));
    }
}
