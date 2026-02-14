package com.BuildBox.BuildServer.service;

import com.BuildBox.BuildServer.model.Deployment;
import com.BuildBox.BuildServer.repository.DeploymentRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Bridges the gap between the build pipeline and persistence/streaming layers.
 * 
 * This service:
 * 1. Updates deployment status in PostgreSQL at each pipeline stage
 * 2. Pushes logs to Redis Streams for real-time SSE consumption by
 * Log_Analytics_Server
 */
@Service
public class DeploymentTrackingService {

    private final DeploymentRepository deploymentRepository;
    private final StringRedisTemplate redisTemplate;

    public DeploymentTrackingService(DeploymentRepository deploymentRepository,
            StringRedisTemplate redisTemplate) {
        this.deploymentRepository = deploymentRepository;
        this.redisTemplate = redisTemplate;
    }

    /**
     * Update the deployment status in the database.
     */
    public void updateStatus(Long deploymentId, String status) {
        if (deploymentId == null)
            return;

        // Retry lookup to handle race condition with Frontend Server commit
        int retries = 3;
        while (retries > 0) {
            var opt = deploymentRepository.findById(deploymentId);
            if (opt.isPresent()) {
                Deployment d = opt.get();
                d.setStatus(status);
                if ("READY".equals(status) || "FAILED".equals(status)) {
                    d.setCompletedAt(LocalDateTime.now());
                }
                deploymentRepository.save(d);
                return;
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException ignore) {
            }
            retries--;
        }
        System.err.println("⚠️ Warning: Could not find deployment " + deploymentId + " to update status to " + status);
    }

    /**
     * Set the deployment URL once the task is running.
     */
    public void setDeploymentUrl(Long deploymentId, String url) {
        if (deploymentId == null)
            return;

        deploymentRepository.findById(deploymentId).ifPresent(deployment -> {
            deployment.setDeploymentUrl(url);
            deploymentRepository.save(deployment);
        });
    }

    /**
     * Log a build message: persists to DB AND pushes to Redis Streams.
     * This bridges the gap between CloudWatch logs and the real-time SSE pipeline.
     */
    public void log(Long deploymentId, String message) {
        if (deploymentId == null) {
            System.out.println("[BUILD] " + message);
            return;
        }

        // 1. Skip PostgreSQL persistence for build logs (as per user request)
        // We only persist the final status in Deployment table via updateStatus()

        // 2. Push to Redis Stream for real-time consumption
        try {
            redisTemplate.opsForStream()
                    .add("logs:" + deploymentId, java.util.Map.of("log", message));
        } catch (Exception e) {
            System.err.println("Failed to push log to Redis Stream: " + e.getMessage());
        }

        System.out.println("[BUILD:" + deploymentId + "] " + message);
    }

    /**
     * Mark a deployment as completed with a final status.
     */
    public void complete(Long deploymentId, boolean success) {
        String status = success ? "READY" : "FAILED";
        updateStatus(deploymentId, status);
        log(deploymentId, "Deployment " + (success ? "completed successfully ✅" : "failed ❌"));
    }
}
