# BuildServer Platform - Quick Start Guide

## 🚨 CRITICAL FIXES NEEDED NOW

### Fix #1: Update EcsService.java for EC2 Launch Type

**Current Problem**: Using Fargate configuration  
**Required**: EC2 + Bridge mode with image overrides

**File**: `src/main/java/com/BuildBox/BuildServer/aws/EcsService.java`

Replace the entire `runTask()` method with this:

```java
public String runTask(String cluster, 
                      String taskFamily,  // "user-node-task" or "user-python-task"
                      String imageUri,     // User's ECR image
                      String containerName, // "user-node-app" or "user-python-app"
                      String projectId) {

    // Container override to inject user's image
    ContainerOverride containerOverride = ContainerOverride.builder()
            .name(containerName)
            .image(imageUri)
            .build();

    // Task override with environment variables
    TaskOverride taskOverride = TaskOverride.builder()
            .containerOverrides(containerOverride)
            .build();

    RunTaskRequest request = RunTaskRequest.builder()
            .cluster(cluster)
            .taskDefinition(taskFamily)  // Use existing task def
            .launchType(LaunchType.EC2)  // ✅ CHANGED: EC2 not Fargate
            .overrides(taskOverride)     // ✅ ADDED: Image override
            .startedBy("buildserver-" + projectId)
            .build();

    RunTaskResponse response = ecs.runTask(request);
    
    if (response.failures() != null && !response.failures().isEmpty()) {
        throw new RuntimeException("ECS task failed: " + response.failures().get(0).reason());
    }

    return response.tasks().get(0).taskArn();
}
```

**Remove this method entirely** (no longer needed):
```java
public String registerTask(...) { /* DELETE THIS */ }
```

---

### Fix #2: Update BuildService.java

**File**: `src/main/java/com/BuildBox/BuildServer/service/BuildService.java`

Replace the `buildAndRun()` method:

```java
public String buildAndRun(String projectId, String runtime) throws Exception {

    Path projectDir = Path.of(BASE_DIR, "downloads", projectId);

    // 1. Download code from S3
    s3.downloadDirectory(BUCKET, projectId + "/", projectDir);

    // 2. Inject Dockerfile
    String dockerfileName = runtime.equals("node") ? "node.dockerfile" : "python.dockerfile";
    CommandExecutor.run(
            "cp " + DOCKERFILES_DIR + "/" + dockerfileName + " " + projectDir + "/Dockerfile"
    );

    // 3. Docker build
    String imageTag = projectId + ":latest";
    CommandExecutor.run("docker build -t " + imageTag + " " + projectDir);

    // 4. Push to ECR
    String repoUri = ecr.ensureRepository(projectId);
    EcrDockerLogin.login();

    CommandExecutor.run("docker tag " + imageTag + " " + repoUri + ":latest");
    CommandExecutor.run("docker push " + repoUri + ":latest");

    // 5. Run ECS task with image override
    String taskFamily = runtime.equals("node") ? "user-node-task" : "user-python-task";
    String containerName = runtime.equals("node") ? "user-node-app" : "user-python-app";
    int containerPort = runtime.equals("node") ? 3000 : 8000;

    String taskArn = ecs.runTask(
            CLUSTER,
            taskFamily,
            repoUri + ":latest",
            containerName,
            projectId
    );

    System.out.println("✅ Task started: " + taskArn);
    return taskArn;  // Return for tracking
}
```

---

### Fix #3: Update application.properties

**File**: `src/main/resources/application.properties`

Add these (replace placeholders with your real values):

```properties
spring.application.name=BuildServer
server.port=8080

# AWS Configuration
aws.region=ap-south-1

# ECS Configuration
ecs.cluster.name=buildserver-cluster-1
ecs.subnet.id=subnet-XXXXXXXXX
ecs.security.group.id=sg-XXXXXXXXX
ecs.task.execution.role.arn=arn:aws:iam::ACCOUNT_ID:role/ecsTaskExecutionRole

# S3 Configuration
s3.bucket.name=backend-artifacts

# Build Configuration
build.base.dir=/BuildServer
build.dockerfiles.dir=/dockerfiles
```

---

### Fix #4: Inject Configuration into BuildService

Add configuration injection to `BuildService.java`:

```java
@Service
public class BuildService {

    @Value("${build.base.dir}")
    private String BASE_DIR;

    @Value("${build.dockerfiles.dir}")
    private String DOCKERFILES_DIR;

    @Value("${s3.bucket.name}")
    private String BUCKET;

    @Value("${ecs.cluster.name}")
    private String CLUSTER;

    @Value("${ecs.subnet.id}")
    private String SUBNET;

    @Value("${ecs.security.group.id}")
    private String SECURITY_GROUP;

    @Value("${ecs.task.execution.role.arn}")
    private String EXEC_ROLE_ARN;

    private final S3Downloader s3 = new S3Downloader();
    private final EcrService ecr = new EcrService();
    private final EcsService ecs = new EcsService();

    // ... rest of the code
}
```

---

## 🧪 TESTING THE FIXES

### Step 1: Build the Application

```bash
cd C:\Users\Kavya\OneDrive\Desktop\BuildServer
mvnw clean package
```

### Step 2: Run the BuildServer

```bash
java -jar target/BuildServer-0.0.1-SNAPSHOT.jar
```

### Step 3: Trigger a Build

**Using curl** (Git Bash or PowerShell):
```bash
curl -X POST http://localhost:8080/api/builds \
  -H "Content-Type: application/json" \
  -d '{"projectId": "test-node-app", "runtime": "node"}'
```

**Using PowerShell**:
```powershell
Invoke-WebRequest -Uri http://localhost:8080/api/builds `
  -Method POST `
  -ContentType "application/json" `
  -Body '{"projectId": "test-node-app", "runtime": "node"}'
```

### Step 4: Verify in AWS Console

1. Go to ECS Console → Clusters → `buildserver-cluster-1`
2. Click "Tasks" tab
3. You should see a task in PENDING or RUNNING state
4. Click the task to see details and assigned port

---

## 📝 NEXT STEPS AFTER FIXING

Once the above fixes work, you need to build:

### Phase 2: Port Discovery Service

**Create**: `src/main/java/com/BuildBox/BuildServer/service/TaskPortDiscoveryService.java`

```java
package com.BuildBox.BuildServer.service;

import software.amazon.awssdk.services.ecs.EcsClient;
import software.amazon.awssdk.services.ecs.model.*;
import org.springframework.stereotype.Service;

@Service
public class TaskPortDiscoveryService {

    private final EcsClient ecs = EcsClient.create();

    public Integer getAssignedPort(String cluster, String taskArn, String containerName) {
        
        DescribeTasksResponse response = ecs.describeTasks(
                DescribeTasksRequest.builder()
                        .cluster(cluster)
                        .tasks(taskArn)
                        .build()
        );

        if (response.tasks().isEmpty()) {
            throw new RuntimeException("Task not found: " + taskArn);
        }

        Task task = response.tasks().get(0);

        // Find the container
        for (software.amazon.awssdk.services.ecs.model.Container container : task.containers()) {
            if (container.name().equals(containerName)) {
                
                // Get network bindings
                for (NetworkBinding binding : container.networkBindings()) {
                    if (binding.hostPort() != null) {
                        return binding.hostPort();
                    }
                }
            }
        }

        throw new RuntimeException("No port binding found for container: " + containerName);
    }

    public boolean isTaskRunning(String cluster, String taskArn) {
        DescribeTasksResponse response = ecs.describeTasks(
                DescribeTasksRequest.builder()
                        .cluster(cluster)
                        .tasks(taskArn)
                        .build()
        );

        if (response.tasks().isEmpty()) {
            return false;
        }

        String status = response.tasks().get(0).lastStatus();
        return "RUNNING".equals(status);
    }
}
```

---

### Phase 3: Task Registry

**Create**: `src/main/java/com/BuildBox/BuildServer/model/TaskInfo.java`

```java
package com.BuildBox.BuildServer.model;

import java.time.Instant;

public record TaskInfo(
    String projectId,
    String taskArn,
    String runtime,
    Integer hostPort,       // null until discovered
    String status,
    String imageUri,
    Instant startedAt
) {}
```

**Create**: `src/main/java/com/BuildBox/BuildServer/service/TaskRegistry.java`

```java
package com.BuildBox.BuildServer.service;

import com.BuildBox.BuildServer.model.TaskInfo;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TaskRegistry {

    private final Map<String, TaskInfo> activeTasks = new ConcurrentHashMap<>();

    public void register(String projectId, String taskArn, String runtime, String imageUri) {
        TaskInfo info = new TaskInfo(
                projectId,
                taskArn,
                runtime,
                null,  // Port discovered later
                "PENDING",
                imageUri,
                Instant.now()
        );
        activeTasks.put(projectId, info);
    }

    public void updatePort(String projectId, Integer port) {
        TaskInfo existing = activeTasks.get(projectId);
        if (existing != null) {
            TaskInfo updated = new TaskInfo(
                    existing.projectId(),
                    existing.taskArn(),
                    existing.runtime(),
                    port,
                    "RUNNING",
                    existing.imageUri(),
                    existing.startedAt()
            );
            activeTasks.put(projectId, updated);
        }
    }

    public Optional<TaskInfo> getTask(String projectId) {
        return Optional.ofNullable(activeTasks.get(projectId));
    }

    public void remove(String projectId) {
        activeTasks.remove(projectId);
    }

    public Collection<TaskInfo> getAllTasks() {
        return activeTasks.values();
    }
}
```

---

### Phase 4: Add Query Endpoint

**Update**: `BuildController.java`

```java
@RestController
@RequestMapping("/api")
public class BuildController {

    private final AsyncBuildExecutor executor;
    private final TaskRegistry taskRegistry;

    public BuildController(AsyncBuildExecutor executor, TaskRegistry taskRegistry) {
        this.executor = executor;
        this.taskRegistry = taskRegistry;
    }

    @PostMapping("/builds")
    public BuildResponse triggerBuild(@Valid @RequestBody BuildRequest request) {
        executor.startBuild(request.getProjectId(), request.getRuntime());
        return new BuildResponse(request.getProjectId(), "BUILD_STARTED");
    }

    @GetMapping("/tasks/{projectId}")
    public ResponseEntity<?> getTaskInfo(@PathVariable String projectId) {
        return taskRegistry.getTask(projectId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/tasks")
    public Collection<TaskInfo> getAllTasks() {
        return taskRegistry.getAllTasks();
    }
}
```

---

## 📊 SUCCESS CRITERIA

After implementing the fixes, you should be able to:

✅ **Phase 1 Complete** when:
- POST /api/builds triggers a build
- Docker image is pushed to ECR
- ECS task starts on your t3.micro
- Task shows RUNNING in ECS console

✅ **Phase 2 Complete** when:
- GET /api/tasks/{projectId} returns task info
- Response includes the dynamically assigned port
- You can manually curl the task: `curl http://EC2_IP:PORT/health`

✅ **Phase 3 Complete** when:
- Nginx or ALB is routing traffic
- Frontend can call `/api/project-id/*` and reach the task

---

## 🆘 TROUBLESHOOTING

### Issue: "No container instances available"
**Solution**: Check that your EC2 instance is registered with the cluster:
```bash
aws ecs list-container-instances --cluster buildserver-cluster-1
```

### Issue: Task stuck in PENDING
**Causes**:
1. Not enough memory (check t3.micro capacity)
2. Image pull failed (check ECR permissions)
3. No available ports (only ~300 ephemeral ports)

**Solution**: Check CloudWatch logs for the task.

### Issue: "Task failed to start"
**Solution**: 
```bash
aws ecs describe-tasks --cluster buildserver-cluster-1 --tasks <TASK_ARN>
```
Look at `stoppedReason` and `containers[].reason`.

---

## 📚 USEFUL AWS CLI COMMANDS

```bash
# List running tasks
aws ecs list-tasks --cluster buildserver-cluster-1

# Describe task to see port
aws ecs describe-tasks --cluster buildserver-cluster-1 --tasks <TASK_ARN>

# Check EC2 instance in cluster
aws ecs describe-container-instances --cluster buildserver-cluster-1 \
  --container-instances <INSTANCE_ID>

# View logs
aws logs tail /ecs/user-node-app --follow
```

---

**Ready? Start with Fix #1 and work your way down. I'm here to help debug!** 🚀
