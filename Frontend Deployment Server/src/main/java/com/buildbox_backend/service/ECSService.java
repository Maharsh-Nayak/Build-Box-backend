package com.buildbox_backend.service;

import com.buildbox_backend.repository.EnvVariableRepository;
import io.lettuce.core.ScriptOutputType;
import jakarta.servlet.annotation.WebServlet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.ecs.EcsClient;
import software.amazon.awssdk.services.ecs.model.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@PropertySource("classpath:credentials.properties")
public class ECSService {

    private final EcsClient ecsClient;
    private final EnvVariableRepository envVariableRepository;


    @Value("${aiven.hostName}")
    private String REDIS_HOST;

    @Value("${aiven.password}")
    private String REDIS_PASSWORD;

    @Autowired
    public ECSService(EcsClient ecsClient, EnvVariableRepository envVariableRepository) {
        this.ecsClient = ecsClient;
        System.out.println("ECS Client created");
        this.envVariableRepository = envVariableRepository;
    }

    public Map<String, String> startBuild(String gitUrl, String projectName, String userId, String backendDir, String fonrtendDir, Map<String, String> customEnvs, Map<String, String> backendEnvVars) {

        System.out.println("Starting build");

        String buildId = UUID.randomUUID().toString();
        System.out.println("Build ID: " + buildId);

        AwsVpcConfiguration vpcConfig = AwsVpcConfiguration.builder()
                .subnets("subnet-07922d63f29663d95", "subnet-050daecddde0e6df9")
                .securityGroups("sg-08873b0b2ce54ac02")
                .assignPublicIp(AssignPublicIp.ENABLED)
                .build();

        NetworkConfiguration networkConfiguration = NetworkConfiguration.builder()
                .awsvpcConfiguration(vpcConfig)
                .build();

        List<KeyValuePair> envVars = new java.util.ArrayList<>(List.of(
                KeyValuePair.builder().name("GIT_URL").value(gitUrl).build(),
                KeyValuePair.builder().name("PROJECT_NAME").value(projectName).build(),
                KeyValuePair.builder().name("USER_ID").value(userId).build(),
                KeyValuePair.builder().name("FRONTENT_DIR").value(fonrtendDir).build(),
                KeyValuePair.builder().name("BACKEND_DIR").value(backendDir).build(),
                KeyValuePair.builder().name("BUILD_ID").value(buildId).build(),
                KeyValuePair.builder().name("REDIS_HOST").value(REDIS_HOST).build(),
                KeyValuePair.builder().name("REDIS_PORT").value("12608").build(),
                KeyValuePair.builder().name("REDIS_PASSWORD").value(REDIS_PASSWORD).build()
        ));

        // 2. Dynamically add the custom frontend variables
        if (customEnvs != null) {
            System.out.println("Custom env variables: ");
            customEnvs.forEach((key, value) -> {
                System.out.println(key + " = " + value);
                envVars.add(KeyValuePair.builder().name("FRONTEND_ENV_"+key.toString()).value(value).build());
            });
        }

        if(backendEnvVars != null) {
            backendEnvVars.forEach((key, value) -> {
                envVars.add(KeyValuePair.builder().name("BACKEND_ENV_" + key.toString()).value(value).build());
            });
        }

        RunTaskRequest request = RunTaskRequest.builder()
                .cluster("outstanding-gecko-uvkkj2")
                .taskDefinition("BuildBoxDeploy:13")
                .launchType(LaunchType.FARGATE)
                .networkConfiguration(networkConfiguration)
        .overrides(TaskOverride.builder()
                .containerOverrides(ContainerOverride.builder()
                        .name("InitialDeploy")
                        .environment(
                                envVars
                        )
                        .build())
                .build())
                .build();

        RunTaskResponse response = ecsClient.runTask(request);

        String taskArn = response.tasks().get(0).taskArn();

        Map<String, String> Ids = new HashMap<>();
        Ids.put("buildId", buildId);
        Ids.put("taskId", taskArn.substring(taskArn.lastIndexOf("/") + 1));

        return Ids;
    }

    public String getTaskStatus(String taskId) {
        DescribeTasksRequest describeRequest = DescribeTasksRequest.builder()
                .cluster("outstanding-gecko-uvkkj2")
                .tasks(taskId)
                .build();

        DescribeTasksResponse response = ecsClient.describeTasks(describeRequest);

        if (response.tasks().isEmpty()) return "NOT_FOUND";

        Task task = response.tasks().get(0);
        String lastStatus = task.lastStatus(); // PROVISIONING, RUNNING, STOPPED

        if ("STOPPED".equals(lastStatus)) {
            // Check the exit code of the first container
            Integer exitCode = task.containers().get(0).exitCode();
            return (exitCode != null && exitCode == 0) ? "SUCCESS" : "FAILED";
        }

        return lastStatus; // Still in progress
    }


}
