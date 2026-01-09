package com.buildbox_backend.controller;

import com.buildbox_backend.dto.GitCloneRequest;
import com.buildbox_backend.service.DeployService;
import com.buildbox_backend.service.ECSService;
import com.buildbox_backend.service.GitCloneService;
import com.buildbox_backend.service.UploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;

@CrossOrigin(origins = "http://localhost:8082")
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
    public ResponseEntity<String> deploy(@RequestBody GitCloneRequest request) {
        System.out.println("Starting deployment for: " + request.getLink());

        // 1. Clone
        File repoDir = gitCloneService.cloneRepository(request.getLink());

        // 2. Build
        deployService.buildProject(repoDir);

        // 3. Upload
        File distFolder = new File(repoDir, "dist");
        // Using a generic project ID "demo" as per original code, or could derive from repo name
        uploadService.uploadDirectory(distFolder, request.getProjectName());

        // 4. Cleanup
        gitCloneService.cleanup(repoDir);
        System.out.println("Deployment finished and cleanup done.");

        // Returning generic response or bucket URL if we had it
        String deployLink = "https://buildbox-frontend.s3.ap-south-1.amazonaws.com/"+request.getProjectName()+"/index.html";
        return ResponseEntity.status(HttpStatus.CREATED).body(deployLink);
    }

}
