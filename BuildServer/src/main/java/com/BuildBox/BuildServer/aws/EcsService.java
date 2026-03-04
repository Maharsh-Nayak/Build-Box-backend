package com.BuildBox.BuildServer.aws;

import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.Reservation;
import software.amazon.awssdk.services.ecs.EcsClient;
import software.amazon.awssdk.services.ecs.model.*;
import org.springframework.stereotype.Service;

@Service
public class EcsService {

    private final EcsClient ecs;
    private final Ec2Client ec2;

    public EcsService(EcsClient ecs, Ec2Client ec2) {
        this.ecs = ecs;
        this.ec2 = ec2;
    }

    // ... existing RunTask method ...

    /**
     * Get the IP address of the EC2 instance running the task.
     */
    public String getContainerInstanceIp(String cluster, String taskArn) {
        // 1. Get Task details -> ContainerInstanceArn
        DescribeTasksResponse taskResp = ecs.describeTasks(
                DescribeTasksRequest.builder()
                        .cluster(cluster)
                        .tasks(taskArn)
                        .build());

        if (taskResp.tasks().isEmpty()) {
            throw new RuntimeException("Task not found: " + taskArn);
        }

        String containerInstanceArn = taskResp.tasks().get(0).containerInstanceArn();
        if (containerInstanceArn == null) {
            throw new RuntimeException("Task is not running on an EC2 instance (Fargate?)");
        }

        // 2. Get Container Instance details -> EC2 Instance ID
        DescribeContainerInstancesResponse ciResp = ecs.describeContainerInstances(
                DescribeContainerInstancesRequest.builder()
                        .cluster(cluster)
                        .containerInstances(containerInstanceArn)
                        .build());

        if (ciResp.containerInstances().isEmpty()) {
            throw new RuntimeException("Container instance not found: " + containerInstanceArn);
        }

        String ec2InstanceId = ciResp.containerInstances().get(0).ec2InstanceId();

        // 3. Get EC2 Instance details -> Public IP
        DescribeInstancesResponse ec2Resp = ec2.describeInstances(
                DescribeInstancesRequest.builder()
                        .instanceIds(ec2InstanceId)
                        .build());

        for (Reservation res : ec2Resp.reservations()) {
            for (Instance inst : res.instances()) {
                if (inst.publicIpAddress() != null)
                    return inst.publicIpAddress();
                if (inst.privateIpAddress() != null)
                    return inst.privateIpAddress();
            }
        }

        return "unknown"; // Should not happen
    }

    // ... runTask ... getAssignedPort ... isTaskRunning ... stopTask methods ...

    /**
     * Run an ECS task using EC2 launch type with image override.
     * Uses pre-registered task definitions (user-node-task or user-python-task)
     * and overrides the container image at runtime.
     *
     * @param cluster       ECS cluster name
     * @param taskFamily    Task definition family (e.g., "user-node-task")
     * @param imageUri      User's ECR image URI
     * @param containerName Container name from task definition
     * @param projectId     Project identifier for tracking
     * @return Task ARN of the started task
     */
    public String runTask(String cluster,
            String taskFamily,
            String imageUri,
            String containerName,
            String projectId) {

        // Note: AWS ECS ContainerOverride does NOT support changing the image.
        // We must register a new task definition revision with the user's image.

        String taskDefArn = registerTaskDefinition(taskFamily, containerName, imageUri, projectId);

        // Determine runtime from taskFamily (e.g., "user-node-task" -> "node")
        String runtime = taskFamily.contains("node") ? "node" : 
                        taskFamily.contains("python") ? "python" : "unknown";

        // Run task with EC2 launch type using new task definition
        RunTaskRequest request = RunTaskRequest.builder()
                .cluster(cluster)
                .taskDefinition(taskDefArn) // Use newly registered task def
                .launchType(LaunchType.EC2) // ✅ EC2 launch type
                .startedBy("buildserver-" + projectId) // For tracking
                .tags(
                    software.amazon.awssdk.services.ecs.model.Tag.builder()
                        .key("ProjectId")
                        .value(projectId)
                        .build(),
                    software.amazon.awssdk.services.ecs.model.Tag.builder()
                        .key("Runtime")
                        .value(runtime)
                        .build(),
                    software.amazon.awssdk.services.ecs.model.Tag.builder()
                        .key("ManagedBy")
                        .value("BuildBox")
                        .build()
                )
                .build();

        RunTaskResponse response = ecs.runTask(request);

        // Check for failures
        if (response.failures() != null && !response.failures().isEmpty()) {
            Failure failure = response.failures().get(0);
            throw new RuntimeException("ECS task failed to start: " + failure.reason());
        }

        if (response.tasks().isEmpty()) {
            throw new RuntimeException("No task was started");
        }

        String taskArn = response.tasks().get(0).taskArn();
        System.out.println("✅ Task started: " + taskArn);

        return taskArn;
    }

    /**
     * Stop a running ECS task.
     * 
     * @param cluster ECS cluster name
     * @param taskArn ARN of the task to stop
     * @param reason  Reason for stopping the task
     */
    public void stopTask(String cluster, String taskArn, String reason) {
        System.out.println("🛑 Stopping task: " + taskArn);

        StopTaskRequest request = StopTaskRequest.builder()
                .cluster(cluster)
                .task(taskArn)
                .reason(reason != null ? reason : "Stopped via BuildBox API")
                .build();

        ecs.stopTask(request);
        System.out.println("✅ Task stop initiated: " + taskArn);
    }

    /**
     * Register a new task definition revision with the user's image.
     * This is required because ContainerOverride does not support changing images.
     */
    private String registerTaskDefinition(String family, String containerName, String imageUri, String projectId) {

        // Define the container with the user's image
        ContainerDefinition container = ContainerDefinition.builder()
                .name(containerName)
                .image(imageUri)
                .memory(256) // 256 MB
                .cpu(128) // 0.125 vCPU
                .essential(true)
                .portMappings(PortMapping.builder()
                        .containerPort(containerName.contains("node") ? 3000 : 5000) // Node=3000, Python=5000
                        .hostPort(0) // Dynamic port mapping for bridge mode
                        .protocol(TransportProtocol.TCP)
                        .build())
                .logConfiguration(LogConfiguration.builder()
                        .logDriver(LogDriver.AWSLOGS)
                        .options(java.util.Map.of(
                                "awslogs-group", "/ecs/" + containerName,
                                "awslogs-region", "ap-south-1",
                                "awslogs-stream-prefix", projectId))
                        .build())
                .build();

        RegisterTaskDefinitionRequest request = RegisterTaskDefinitionRequest.builder()
                .family(family)
                .networkMode(NetworkMode.BRIDGE)
                .requiresCompatibilities(Compatibility.EC2)
                .containerDefinitions(container)
                .build();

        RegisterTaskDefinitionResponse response = ecs.registerTaskDefinition(request);
        String taskDefArn = response.taskDefinition().taskDefinitionArn();

        System.out.println("📋 Registered task definition: " + taskDefArn);
        return taskDefArn;
    }

    /**
     * Get the dynamically assigned port for a running task.
     * Bridge mode assigns dynamic port mappings (hostPort: 0 → actual port).
     *
     * @param cluster       ECS cluster name
     * @param taskArn       Task ARN
     * @param containerName Container name to find port for
     * @return Assigned host port (e.g., 32768)
     */
    public Integer getAssignedPort(String cluster, String taskArn, String containerName) {

        DescribeTasksResponse response = ecs.describeTasks(
                DescribeTasksRequest.builder()
                        .cluster(cluster)
                        .tasks(taskArn)
                        .build());

        if (response.tasks().isEmpty()) {
            throw new RuntimeException("Task not found: " + taskArn);
        }

        Task task = response.tasks().get(0);

        // Wait for task to be RUNNING before checking ports
        if (!"RUNNING".equals(task.lastStatus())) {
            throw new RuntimeException("Task not yet running. Status: " + task.lastStatus());
        }

        // Find the container and extract network binding
        for (Container container : task.containers()) {
            if (container.name().equals(containerName)) {

                for (NetworkBinding binding : container.networkBindings()) {
                    if (binding.hostPort() != null) {
                        return binding.hostPort();
                    }
                }
            }
        }

        throw new RuntimeException("No port binding found for container: " + containerName);
    }

    /**
     * Check if a task is in RUNNING state.
     */
    public boolean isTaskRunning(String cluster, String taskArn) {

        DescribeTasksResponse response = ecs.describeTasks(
                DescribeTasksRequest.builder()
                        .cluster(cluster)
                        .tasks(taskArn)
                        .build());

        if (response.tasks().isEmpty()) {
            return false;
        }

        String status = response.tasks().get(0).lastStatus();
        return "RUNNING".equals(status);
    }

    /**
     * Get the current status of a task.
     */
    public String getTaskStatus(String cluster, String taskArn) {
        DescribeTasksResponse response = ecs.describeTasks(
                DescribeTasksRequest.builder()
                        .cluster(cluster)
                        .tasks(taskArn)
                        .build());

        if (response.tasks().isEmpty()) {
            return "UNKNOWN";
        }

        return response.tasks().get(0).lastStatus();
    }
}
