package com.BuildBox.BuildServer.service;

import com.BuildBox.BuildServer.model.TaskInfo;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class TaskRegistry {

    private static final String REDIS_KEY_PREFIX = "buildbox:task:";
    private final RedisTemplate<String, TaskInfo> redisTemplate;

    public TaskRegistry(RedisTemplate<String, TaskInfo> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void register(String projectId, String taskArn, String runtime, String host, int port) {
        TaskInfo task = new TaskInfo(
                projectId,
                taskArn,
                runtime,
                host,
                port,
                "RUNNING",
                Instant.now(),
                Instant.now() // Initial last activity
        );
        redisTemplate.opsForValue().set(REDIS_KEY_PREFIX + projectId, task, 24, TimeUnit.HOURS);
        System.out.println("📝 Redis: Registered task for project: " + projectId + " at " + host + ":" + port);
    }

    public void updateActivity(String projectId) {
        TaskInfo v = getTask(projectId);
        if (v != null) {
            TaskInfo updated = new TaskInfo(
                    v.projectId(),
                    v.taskArn(),
                    v.runtime(),
                    v.host(),
                    v.hostPort(),
                    v.status(),
                    v.startedAt(),
                    Instant.now());
            redisTemplate.opsForValue().set(REDIS_KEY_PREFIX + projectId, updated, 24, TimeUnit.HOURS);
        }
    }

    public TaskInfo getTask(String projectId) {
        return redisTemplate.opsForValue().get(REDIS_KEY_PREFIX + projectId);
    }

    public Collection<TaskInfo> getAllTasks() {
        Collection<String> keys = redisTemplate.keys(REDIS_KEY_PREFIX + "*");
        if (keys == null || keys.isEmpty())
            return java.util.Collections.emptyList();

        java.util.List<TaskInfo> tasks = redisTemplate.opsForValue().multiGet(keys);
        if (tasks == null)
            return java.util.Collections.emptyList();

        return tasks.stream()
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
    }

    public void removeTask(String projectId) {
        redisTemplate.delete(REDIS_KEY_PREFIX + projectId);
        System.out.println("🗑️ Redis: Removed task for project: " + projectId);
    }
}
