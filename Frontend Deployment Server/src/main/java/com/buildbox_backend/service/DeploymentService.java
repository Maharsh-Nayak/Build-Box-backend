package com.buildbox_backend.service;

import com.buildbox_backend.model.Deployment;
import com.buildbox_backend.model.Project;
import com.buildbox_backend.repository.DeploymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class DeploymentService {

    @Autowired
    private DeploymentRepository deploymentRepository;

    @Autowired
    private ActivityService activityService;

    @Value("${buildserver.url:http://localhost:9191}")
    private String buildServerUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Create a new deployment for a project and trigger the build on the
     * BuildServer.
     */
    public Deployment triggerDeployment(Project project, String branch, String commitId, String commitMessage) {
        // Auto-increment version
        int nextVersion = deploymentRepository.findTopByProjectIdOrderByVersionDesc(project.getId())
                .map(d -> d.getVersion() + 1)
                .orElse(1);

        Deployment deployment = new Deployment();
        deployment.setProject(project);
        deployment.setVersion(nextVersion);
        deployment.setStatus("QUEUED");
        deployment.setBranch(branch != null ? branch : "main");
        deployment.setCommitId(commitId);
        deployment.setCommitMessage(commitMessage);
        deployment.setCreatedAt(LocalDateTime.now());

        Deployment saved = deploymentRepository.save(deployment);
        deploymentRepository.flush();

        // Trigger the build on BuildServer asynchronously
        try {
            Map<String, Object> buildRequest = Map.of(
                    "projectId", project.getSlug(),
                    "runtime", "node",
                    "deploymentId", saved.getId(),
                    "basePath", project.getBasePath());
            String endpoint = "/api/builds";
            if ("test-fullstack-app".equals(project.getSlug())) {
                endpoint = "/api/builds/test-local";
                System.out.println("ℹ️ Using LOCAL build for test-fullstack-app");
            }

            restTemplate.postForEntity(buildServerUrl + endpoint, buildRequest, Map.class);
        } catch (Exception e) {
            System.err.println("❌ ERROR: Failed to trigger build on BuildServer at " + buildServerUrl);
            e.printStackTrace();
            // If BuildServer is unreachable, mark as FAILED
            saved.setStatus("FAILED");
            saved.setCompletedAt(LocalDateTime.now());
            deploymentRepository.save(saved);
        }

        activityService.log(
                project.getUser() != null ? project.getUser().getId() : null,
                project.getId(),
                "Triggered deployment v" + nextVersion);

        return saved;
    }

    public List<Deployment> getDeploymentsForProject(Long projectId) {
        return deploymentRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
    }

    public Optional<Deployment> getById(Long id) {
        return deploymentRepository.findById(id);
    }

    public Deployment updateStatus(Long deploymentId, String status) {
        Deployment deployment = deploymentRepository.findById(deploymentId)
                .orElseThrow(() -> new RuntimeException("Deployment not found: " + deploymentId));

        deployment.setStatus(status);
        if ("READY".equals(status) || "FAILED".equals(status)) {
            deployment.setCompletedAt(LocalDateTime.now());
        }
        return deploymentRepository.save(deployment);
    }
}
