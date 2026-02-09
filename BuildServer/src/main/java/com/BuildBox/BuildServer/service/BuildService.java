package com.BuildBox.BuildServer.service;

import com.BuildBox.BuildServer.aws.EcrService;
import com.BuildBox.BuildServer.aws.EcsService;
import com.BuildBox.BuildServer.util.*;
import com.BuildBox.BuildServer.service.TaskPortDiscoveryService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Path;

@Service
public class BuildService {

    @Value("${build.base.dir:C:/Users/Kavya/OneDrive/Desktop/BuildServer}")
    private String BASE_DIR;

    @Value("${build.dockerfiles.dir:C:/Users/Kavya/OneDrive/Desktop/BuildServer/dockerfiles}")
    private String DOCKERFILES_DIR;

    @Value("${s3.bucket.name:backend-artifacts}")
    private String BUCKET;

    @Value("${ecs.cluster.name:buildserver-cluster-1}")
    private String CLUSTER;

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

        Path projectDir = Path.of(BASE_DIR, "downloads", projectId);

        // 1. Download code from S3
        System.out.println("📥 Downloading source code from S3...");
        s3.downloadDirectory(BUCKET, projectId + "/", projectDir);

        // 2. Inject appropriate Dockerfile
        String dockerfileName = runtime.equals("node") ? "node.dockerfile" : "python.dockerfile";
        System.out.println("📝 Injecting Dockerfile: " + dockerfileName);
        
        commandRunner.run(
                "copy \"" + DOCKERFILES_DIR + "\\" + dockerfileName + "\" \"" + projectDir + "\\Dockerfile\""
        );

        // 3. Build Docker image
        String imageTag = projectId + ":latest";
        System.out.println("🔨 Building Docker image: " + imageTag);
        commandRunner.run("docker build -t " + imageTag + " \"" + projectDir + "\"");

        // 4. Push to ECR
        System.out.println("☁️ Pushing to ECR...");
        String repoUri = ecr.ensureRepository(projectId);
        dockerLogin.login();

        commandRunner.run("docker tag " + imageTag + " " + repoUri + ":latest");
        commandRunner.run("docker push " + repoUri + ":latest");

        // 5. Run ECS task with image override
        String taskFamily = runtime.equals("node") ? "user-node-task" : "user-python-task";
        String containerName = runtime.equals("node") ? "user-node-app" : "user-python-app";

        System.out.println("🚀 Starting ECS task...");
        System.out.println("   Task Family: " + taskFamily);
        System.out.println("   Container: " + containerName);
        System.out.println("   Image: " + repoUri + ":latest");

        String taskArn = ecs.runTask(
                CLUSTER,
                taskFamily,
                repoUri + ":latest",
                containerName,
                projectId
        );
        
        System.out.println("✅ Task started: " + taskArn);
        
        // 6. Wait for task to be RUNNING and discover port
        // Note: This blocks the async thread, which is fine.
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

    /**
     * Build from LOCAL code (skips S3 download) - for testing.
     * Assumes code already exists at downloads/{projectId}/
     */
    public String buildAndRunLocal(String projectId, String runtime) throws Exception {

        Path projectDir = Path.of(BASE_DIR, "downloads", projectId);

        System.out.println("📂 Using LOCAL source code at: " + projectDir);

        // 1. Inject appropriate Dockerfile
        String dockerfileName = runtime.equals("node") ? "node.dockerfile" : "python.dockerfile";
        System.out.println("📝 Injecting Dockerfile: " + dockerfileName);
        
        commandRunner.run(
                "copy \"" + DOCKERFILES_DIR + "\\" + dockerfileName + "\" \"" + projectDir + "\\Dockerfile\""
        );

        // 2. Build Docker image
        String imageTag = projectId + ":latest";
        System.out.println("🔨 Building Docker image: " + imageTag);
        commandRunner.run("docker build -t " + imageTag + " \"" + projectDir + "\"");

        // 3. Push to ECR
        System.out.println("☁️ Pushing to ECR...");
        String repoUri = ecr.ensureRepository(projectId);
        dockerLogin.login();

        commandRunner.run("docker tag " + imageTag + " " + repoUri + ":latest");
        commandRunner.run("docker push " + repoUri + ":latest");

        // 4. Run ECS task
        String taskFamily = runtime.equals("node") ? "user-node-task" : "user-python-task";
        String containerName = runtime.equals("node") ? "user-node-app" : "user-python-app";

        System.out.println("🚀 Starting ECS task...");
        System.out.println("   Task Family: " + taskFamily);
        System.out.println("   Container: " + containerName);
        System.out.println("   Image: " + repoUri + ":latest");

        String taskArn = ecs.runTask(
                CLUSTER,
                taskFamily,
                repoUri + ":latest",
                containerName,
                projectId
        );
        
        System.out.println("✅ Task started: " + taskArn);
        
        // 5. Wait for task to be RUNNING and discover port
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
