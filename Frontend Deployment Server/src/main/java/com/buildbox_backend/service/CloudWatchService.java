package com.buildbox_backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;
import software.amazon.awssdk.services.cloudwatchlogs.model.GetLogEventsRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.OutputLogEvent;
import software.amazon.awssdk.services.cloudwatchlogs.model.ResourceNotFoundException;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CloudWatchService {

    private CloudWatchLogsClient cloudWatchLogsClient;

    @Autowired
    public CloudWatchService(CloudWatchLogsClient cloudWatchLogsClient) {
        this.cloudWatchLogsClient = cloudWatchLogsClient;
        System.out.println("CloudWatch Client created");
    }

    public List<String> getLogs(String logGroupName, String logStreamName) {

        try{
            GetLogEventsRequest request = GetLogEventsRequest.builder()
                    .logGroupName(logGroupName)
                    .logStreamName(logStreamName)
                    .build();

            return cloudWatchLogsClient.getLogEvents(request).events().stream().map(OutputLogEvent::message).collect(Collectors.toList());
        }catch (ResourceNotFoundException e) {
            // This is expected during the first few seconds of a task
            System.out.println("Log stream not found yet, retrying... " + logStreamName);
            return Collections.emptyList();
        } catch (Exception e) {
            // Handle other AWS errors
            return List.of("Error fetching logs: " + e.getMessage());
        }

    }

}
