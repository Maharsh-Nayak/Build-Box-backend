package com.BuildBox.BuildServer.service;

import com.BuildBox.BuildServer.aws.EcrService;
import com.BuildBox.BuildServer.aws.EcsService;
import com.BuildBox.BuildServer.util.*;

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
    private final DeploymentTrackingService tracker;

    public BuildService(S3Downloader s3,
            EcrService ecr,
            EcsService ecs,
            TaskPortDiscoveryService discoveryService,
            CommandRunner commandRunner,
            EcrDockerLogin dockerLogin,
            DeploymentTrackingService tracker) {
        this.s3 = s3;
        this.ecr = ecr;
        this.ecs = ecs;
        this.discoveryService = discoveryService;
        this.commandRunner = commandRunner;
        this.dockerLogin = dockerLogin;
        this.tracker = tracker;
    }

    /**
     * Build Docker image and run as ECS task.
     * Returns the task ARN for tracking.
     */
    public String buildAndRun(String projectId, String runtime, Long deploymentId, String basePath) throws Exception {
        Path projectDir = Path.of(BASE_DIR, projectId);

        tracker.log(deploymentId, "📥 Downloading source code from S3...");
        tracker.updateStatus(deploymentId, "BUILDING");

        try{
            s3.downloadDirectory(BUCKET, basePath, projectDir);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return buildFromDirectory(projectId, runtime, projectDir, deploymentId, null);
    }

    /**
     * Build from LOCAL code (skips S3 download) - for testing.
     */
    public String buildAndRunLocal(String projectId, String runtime, Long deploymentId, String basePath)
            throws Exception {
        Path projectDir = Path.of(BASE_DIR, projectId);

        if (!Files.exists(projectDir)) {
            throw new RuntimeException("Project directory not found: " + projectDir);
        }

        tracker.log(deploymentId, "📂 Using LOCAL source code at: " + projectDir);
        tracker.updateStatus(deploymentId, "BUILDING");

        return buildFromDirectory(projectId, runtime, projectDir, deploymentId, basePath);
    }

    // Backwards-compatible overloads
    // Backwards-compatible overloads
    public String buildAndRun(String projectId, String runtime, Long deploymentId) throws Exception {
        return buildAndRun(projectId, runtime, deploymentId, null);
    }

    public String buildAndRun(String projectId, String runtime) throws Exception {
        return buildAndRun(projectId, runtime, null, null);
    }

    public String buildAndRunLocal(String projectId, String runtime, Long deploymentId) throws Exception {
        return buildAndRunLocal(projectId, runtime, deploymentId, null);
    }

    public String buildAndRunLocal(String projectId, String runtime) throws Exception {
        return buildAndRunLocal(projectId, runtime, null, null);
    }

    /**
     * Core build logic - shared by both S3 and local builds.
     */
    private String buildFromDirectory(String projectId, String runtime, Path projectDir, Long deploymentId,
            String basePath)
            throws Exception {

        Path buildContext = projectDir;
//        basePath = "";
        if (basePath != null && !basePath.isEmpty()) {
            buildContext = projectDir.resolve(basePath);
            tracker.log(deploymentId, "📂 Setting build context to: " + basePath);
            System.out.println(buildContext);
        }

        // 1. Copy appropriate Dockerfile
        String dockerfileName = runtime.equals("node") ? "node.dockerfile" : "python.dockerfile";
        Path dockerfileSource = Path.of(DOCKERFILES_DIR, dockerfileName).toAbsolutePath();
        Path dockerfileTarget = buildContext.resolve("Dockerfile").toAbsolutePath();

        System.out.println("DEBUG: dockerfileSource = " + dockerfileSource);
        System.out.println("DEBUG: dockerfileTarget = " + dockerfileTarget);
        System.out.println("DEBUG: buildContext = " + buildContext.toAbsolutePath());

        tracker.log(deploymentId, "📝 Injecting Dockerfile: " + dockerfileName);
        try {
            Files.createDirectories(dockerfileTarget.getParent());
            Files.copy(dockerfileSource, dockerfileTarget, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            tracker.log(deploymentId, "❌ Dockerfile injection failed: " + e.getMessage());
            throw e;
        }

        // 2. Build Docker image with linux/amd64 platform (CRITICAL for Mac → EC2)
        String imageTag = projectId + ":latest";
        tracker.log(deploymentId, "🔨 Building Docker image: " + imageTag + " (platform: linux/amd64)");

        String buildCommand = String.format(
                "docker build --platform linux/amd64 -t %s \"%s\"",
                imageTag,
                buildContext.toAbsolutePath());
        commandRunner.run(buildCommand);

        // 3. Login to ECR
        tracker.log(deploymentId, "🔐 Authenticating with ECR...");
        try {
            dockerLogin.login();
        } catch (Exception e) {
            tracker.log(deploymentId, "❌ ECR Login failed: " + e.getMessage());
            throw e;
        }

        // 4. Tag and push to ECR
        String ecrImageUri = ECR_REGISTRY_URI + "/" + ECR_REPOSITORY_NAME + ":" + projectId;
        tracker.log(deploymentId, "☁️ Pushing to ECR: " + ecrImageUri);

        commandRunner.run("docker tag " + imageTag + " " + ecrImageUri);
        commandRunner.run("docker push " + ecrImageUri);

        // 5. Run ECS task
        String taskFamily = runtime.equals("node") ? "user-node-task" : "user-python-task";
        String containerName = runtime.equals("node") ? "user-node-app" : "user-python-app";

        tracker.log(deploymentId,
                "🚀 Starting ECS task (family: " + taskFamily + ", container: " + containerName + ")");
        tracker.updateStatus(deploymentId, "DEPLOYING");

        String taskArn = ecs.runTask(
                CLUSTER,
                taskFamily,
                ecrImageUri,
                containerName,
                projectId);

        tracker.log(deploymentId, "✅ Task started: " + taskArn);

        // 6. Wait for task to be RUNNING and discover port
        discoveryService.discoverAndRegister(
                CLUSTER,
                taskArn,
                projectId,
                runtime,
                containerName);

        // Set deployment URL and mark as complete
        tracker.setDeploymentUrl(deploymentId, projectId + ".localhost");
        tracker.complete(deploymentId, true);

        return taskArn;
    }
}
