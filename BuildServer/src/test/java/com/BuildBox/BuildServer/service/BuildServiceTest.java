package com.BuildBox.BuildServer.service;

import com.BuildBox.BuildServer.aws.EcrService;
import com.BuildBox.BuildServer.aws.EcsService;
import com.BuildBox.BuildServer.model.TaskInfo;
import com.BuildBox.BuildServer.util.CommandRunner;
import com.BuildBox.BuildServer.util.EcrDockerLogin;
import com.BuildBox.BuildServer.util.S3Downloader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BuildServiceTest {

    @Mock
    private S3Downloader s3;
    @Mock
    private EcrService ecr;
    @Mock
    private EcsService ecs;
    @Mock
    private TaskPortDiscoveryService discoveryService;
    @Mock
    private CommandRunner commandRunner;
    @Mock
    private EcrDockerLogin dockerLogin;

    @InjectMocks
    private BuildService buildService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(buildService, "BASE_DIR", "build_dir");
        ReflectionTestUtils.setField(buildService, "DOCKERFILES_DIR", "docker_dir");
        ReflectionTestUtils.setField(buildService, "BUCKET", "test-bucket");
        ReflectionTestUtils.setField(buildService, "CLUSTER", "test-cluster");
    }

    @Test
    void testBuildAndRunV2() throws Exception {
        // Arrange
        String projectId = "proj-123";
        String runtime = "node";
        String repoUri = "123.dkr.ecr.region.amazonaws.com/repo";
        String taskArn = "arn:aws:ecs:task/123";

        when(ecr.ensureRepository(projectId)).thenReturn(repoUri);
        when(ecs.runTask(anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(taskArn);
        
        TaskInfo mockTask = new TaskInfo(projectId, taskArn, runtime, "1.2.3.4", 8080, "RUNNING", Instant.now());
        when(discoveryService.discoverAndRegister(anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(mockTask);

        // Act
        String resultArn = buildService.buildAndRun(projectId, runtime);

        // Assert
        assertEquals(taskArn, resultArn);

        // Verify flow
        verify(s3).downloadDirectory(eq("test-bucket"), eq(projectId + "/"), any());
        verify(commandRunner, atLeastOnce()).run(contains("docker build"));
        verify(ecr).ensureRepository(projectId);
        verify(dockerLogin).login();
        verify(commandRunner, atLeastOnce()).run(contains("docker push"));
        verify(ecs).runTask(eq("test-cluster"), eq("user-node-task"), eq(repoUri + ":latest"), eq("user-node-app"), eq(projectId));
        verify(discoveryService).discoverAndRegister(eq("test-cluster"), eq(taskArn), eq(projectId), eq(runtime), eq("user-node-app"));
    }
}
