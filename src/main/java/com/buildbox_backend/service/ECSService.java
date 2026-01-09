package com.buildbox_backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.ecs.EcsClient;
import software.amazon.awssdk.services.ecs.model.*;

@Service
public class ECSService {

    private final EcsClient ecsClient;

    @Autowired
    public ECSService(EcsClient ecsClient) {
        this.ecsClient = ecsClient;
        System.out.println("ECS Client created");
    }

    public void startBuild(String gitUrl, String projectName, String userId, String backendDir, String fonrtendDir) {

        System.out.println("Starting build");

        AwsVpcConfiguration vpcConfig = AwsVpcConfiguration.builder()
                .subnets("subnet-07922d63f29663d95", "subnet-050daecddde0e6df9")
                .securityGroups("sg-08873b0b2ce54ac02")
                .assignPublicIp(AssignPublicIp.ENABLED)
                .build();

        NetworkConfiguration networkConfiguration = NetworkConfiguration.builder()
                .awsvpcConfiguration(vpcConfig)
                .build();

        RunTaskRequest request = RunTaskRequest.builder()
                .cluster("outstanding-gecko-uvkkj2")
                .taskDefinition("BuildBoxDeploy:1")
                .launchType(LaunchType.FARGATE)
                .networkConfiguration(networkConfiguration)
        .overrides(TaskOverride.builder()
                .containerOverrides(ContainerOverride.builder()
                        .name("InitialDeploy")
                        .environment(
                                KeyValuePair.builder().name("GIT_URL").value(gitUrl).build(),
                                KeyValuePair.builder().name("PROJECT_NAME").value(projectName).build(),
                                KeyValuePair.builder().name("USER_ID").value(userId).build(),
                                KeyValuePair.builder().name("FRONTENT_DIR").value(fonrtendDir).build(),
                                KeyValuePair.builder().name("BACKEND_DIR").value(backendDir).build()
                        )
                        .build())
                .build())
                .build();

        ecsClient.runTask(request);
    }


}
