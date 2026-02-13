package com.BuildBox.BuildServer.model;

import java.io.Serializable;
import java.time.Instant;

public record TaskInfo(
                String projectId,
                String taskArn,
                String runtime,
                String host,
                int hostPort,
                String status,
                Instant startedAt,
                Instant lastActivity) implements Serializable {
}
