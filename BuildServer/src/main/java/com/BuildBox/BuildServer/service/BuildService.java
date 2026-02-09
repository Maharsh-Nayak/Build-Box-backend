package com.BuildBox.BuildServer.service;

import com.BuildBox.BuildServer.aws.EcrService;
import com.BuildBox.BuildServer.aws.EcsService;
import com.BuildBox.BuildServer.util.*;
import com.BuildBox.BuildServer.service.TaskPortDiscoveryService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Service
public class BuildService {

    @Value("${build.base.dir:/tmp/builds}")
    private String BASE_DIR;

    @Value("${build.dockerfiles.dir:/tmp/dockerfiles}")
    private String DOCKERFILES_DIR;

    @Value("${s3.bucket.name:backend-artifacts}")
    private String BUCKET;

    @Value("${ecs.cluster.name:buildserver-cluster-1}")
    private String CLUSTER;

    @Value("${ecr.registry.uri:}")
    private String ECR_REGISTRY_URI;

    @Value("${ecr.repository.name:buildbox/buildserver}")
    private String ECR_REPOSITORY_NAME;

    private final S3Downloader s3;
    private final EcrService ecr;
    private final EcsService ecs;
    private final TaskPortDiscoveryService discoveryService;
    private final CommandRunner commandRunner;
    private final EcrDockerLogin dockerLogin;

    public BuildService(S3Downloader s3, 
                        EcrService ecr,
                        EcsService ecs, 
                        TaskPortDiscoveryService discoveryService,
                        CommandRunner commandRunner,
                        EcrDockerLogin dockerLogin) {
        this.s3 = s3;
        this.ecr = ecr;
        this.ecs = ecs;
        this.discoveryService = discoveryService;
        this.commandRunner = commandRunner;
        this.dockerLogin = dockerLogin;
    }

    /**
     * Build Docker image and run as ECS task.
     * Returns the task ARN for tracking.
     */
    public String buildAndRun(String projectId, String runtime) throws Exception {

        Path projectDir = Path.of(BASE_DIR, projectId);

        // 1. Download code from S3
        System.out.println("📥 Downloading source code from S3...");
        s3.downloadDirectory(BUCKET, projectId + "/", projectDir);

        return buildFromDirectory(projectId, runtime, projectDir);
    }

    /**
     * Build from LOCAL code (skips S3 download) - for testing.
     * Assumes code already exists at {projectId}/ directory under builds.
     */
    public String buildAndRunLocal(String projectId, String runtime) throws Exception {

        Path projectDir = Path.of(BASE_DIR, projectId);

        if (!Files.exists(projectDir)) {
            throw new RuntimeException("Project directory not found: " + projectDir);
        }

        System.out.println("📂 Using LOCAL source code at: " + projectDir);

        return buildFromDirectory(projectId, runtime, projectDir);
    }

    /**
     * Core build logic - shared by both S3 and local builds.
     */
    private String buildFromDirectory(String projectId, String runtime, Path projectDir) throws Exception {
        
        // 1. Copy appropriate Dockerfile to project directory
        String dockerfileName = runtime.equals("node") ? "node.dockerfile" : "python.dockerfile";
        Path dockerfileSource = Path.of(DOCKERFILES_DIR, dockerfileName);
        Path dockerfileTarget = projectDir.resolve("Dockerfile");
        
        System.out.println("📝 Injecting Dockerfile: " + dockerfileName);
        Files.copy(dockerfileSource, dockerfileTarget, StandardCopyOption.REPLACE_EXISTING);

        // 2. Build Docker image with linux/amd64 platform (CRITICAL for Mac → EC2)
        String imageTag = projectId + ":latest";
        System.out.println("🔨 Building Docker image: " + imageTag);
        System.out.println("   Platform: linux/amd64 (for EC2 compatibility)");
        
        String buildCommand = String.format(
            "docker build --platform linux/amd64 -t %s \"%s\"",
            imageTag,
            projectDir.toAbsolutePath()
        );
        commandRunner.run(buildCommand);

        // 3. Login to ECR
        System.out.println("🔐 Authenticating with ECR...");
        dockerLogin.login();

        // 4. Tag and push to ECR
        // Use the shared ECR repository with image tag for each project
        String ecrImageUri = ECR_REGISTRY_URI + "/" + ECR_REPOSITORY_NAME + ":" + projectId;
        
        System.out.println("☁️ Pushing to ECR...");
        System.out.println("   Image: " + ecrImageUri);
        
        commandRunner.run("docker tag " + imageTag + " " + ecrImageUri);
        commandRunner.run("docker push " + ecrImageUri);

        // 5. Run ECS task (registers new task definition with the image)
        String taskFamily = runtime.equals("node") ? "user-node-task" : "user-python-task";
        String containerName = runtime.equals("node") ? "user-node-app" : "user-python-app";

        System.out.println("🚀 Starting ECS task...");
        System.out.println("   Task Family: " + taskFamily);
        System.out.println("   Container: " + containerName);
        System.out.println("   Image: " + ecrImageUri);

        String taskArn = ecs.runTask(
                CLUSTER,
                taskFamily,
                ecrImageUri,
                containerName,
                projectId
        );
        
        System.out.println("✅ Task started: " + taskArn);
        
        // 6. Wait for task to be RUNNING and discover port
        discoveryService.discoverAndRegister(
                CLUSTER,
                taskArn,
                projectId,
                runtime,
                containerName
        );

        System.out.println("✅ Build and deployment complete!");

        return taskArn;
    }
}
