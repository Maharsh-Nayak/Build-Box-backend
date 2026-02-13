package com.BuildBox.BuildServer.aws;

import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;
import software.amazon.awssdk.services.cloudwatchlogs.model.GetLogEventsRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.GetLogEventsResponse;
import software.amazon.awssdk.services.cloudwatchlogs.model.OutputLogEvent;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CloudWatchLogsService {

    private final CloudWatchLogsClient cloudWatchLogsClient;

    public CloudWatchLogsService(CloudWatchLogsClient cloudWatchLogsClient) {
        this.cloudWatchLogsClient = cloudWatchLogsClient;
    }

    /**
     * Fetch logs for a specific project from CloudWatch.
     * 
     * @param logGroupName  Name of the log group (e.g., "/ecs/user-node-app")
     * @param logStreamName Name of the log stream (e.g.,
     *                      "{projectId}/container/...")
     * @return List of log message strings
     */
    public List<String> getLogs(String logGroupName, String logStreamName) {
        try {
            GetLogEventsRequest request = GetLogEventsRequest.builder()
                    .logGroupName(logGroupName)
                    .logStreamName(logStreamName)
                    .limit(100)
                    .startFromHead(false) // Get recent logs
                    .build();

            GetLogEventsResponse response = cloudWatchLogsClient.getLogEvents(request);

            return response.events().stream()
                    .map(OutputLogEvent::message)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("❌ Failed to fetch logs from CloudWatch: " + e.getMessage());
            return List.of("Error: Could not retrieve logs. " + e.getMessage());
        }
    }
}
