package com.buildbox_backend.controller;

import com.buildbox_backend.dto.GitCloneRequest;
import com.buildbox_backend.service.DeployService;
import com.buildbox_backend.service.GitCloneService;
import com.buildbox_backend.service.UploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;

@RestController
@RequestMapping("/deploy")
public class DeployController {

    private final GitCloneService gitCloneService;
    private final DeployService deployService;
    private final UploadService uploadService;

    @Autowired
    public DeployController(GitCloneService gitCloneService, DeployService deployService, UploadService uploadService) {
        this.gitCloneService = gitCloneService;
        this.deployService = deployService;
        this.uploadService = uploadService;
    }

    @PostMapping
    public String deploy(@RequestBody GitCloneRequest request) {
        System.out.println("Starting deployment for: " + request.getLink());

        // 1. Clone
        File repoDir = gitCloneService.cloneRepository(request.getLink());

        // 2. Build
        deployService.buildProject(repoDir);

        // 3. Upload
        File distFolder = new File(repoDir, "dist");
        // Using a generic project ID "demo" as per original code, or could derive from repo name
        uploadService.uploadDirectory(distFolder, "demo");

        // 4. Cleanup
        gitCloneService.cleanup(repoDir);
        System.out.println("Deployment finished and cleanup done.");

        // Returning generic response or bucket URL if we had it
        return "Deployment successful";
    }
}
