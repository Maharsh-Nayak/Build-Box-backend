package com.buildbox_backend.controller;

import com.buildbox_backend.model.Deployment;
import com.buildbox_backend.model.Project;
import com.buildbox_backend.service.DeploymentService;
import com.buildbox_backend.service.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects/{slug}/deployments")
public class DeploymentController {

    @Autowired
    private DeploymentService deploymentService;

    @Autowired
    private ProjectService projectService;

    @PostMapping
    public ResponseEntity<?> triggerDeployment(@PathVariable String slug,
            @RequestBody(required = false) Map<String, String> body) {
        Project project = projectService.getBySlug(slug).orElse(null);
        if (project == null) {
            return ResponseEntity.notFound().build();
        }

        String branch = body != null ? body.get("branch") : "main";
        String commitId = body != null ? body.get("commitId") : null;
        String commitMessage = body != null ? body.get("commitMessage") : null;

        Deployment deployment = deploymentService.triggerDeployment(project, branch, commitId, commitMessage);
        return ResponseEntity.accepted().body(deployment);
    }

    @GetMapping
    public ResponseEntity<List<Deployment>> listDeployments(@PathVariable String slug) {
        Project project = projectService.getBySlug(slug).orElse(null);
        if (project == null)
            return ResponseEntity.notFound().build();

        return ResponseEntity.ok(deploymentService.getDeploymentsForProject(project.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getDeployment(@PathVariable String slug, @PathVariable Long id) {
        return deploymentService.getById(id)
                .map(d -> ResponseEntity.ok((Object) d))
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable String slug,
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String status = body.get("status");
        if (status == null)
            return ResponseEntity.badRequest().body(Map.of("error", "status required"));

        Deployment updated = deploymentService.updateStatus(id, status);
        return ResponseEntity.ok(updated);
    }
}
