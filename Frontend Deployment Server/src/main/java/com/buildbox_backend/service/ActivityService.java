package com.buildbox_backend.service;

import com.buildbox_backend.model.Activity;
import com.buildbox_backend.repository.ActivityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ActivityService {

    @Autowired
    private ActivityRepository activityRepository;

    public void log(Long userId, Long projectId, String action) {
        Activity activity = new Activity();
        activity.setUserId(userId);
        activity.setProjectId(projectId);
        activity.setAction(action);
        activity.setCreatedAt(LocalDateTime.now());
        activityRepository.save(activity);
    }

    public List<Activity> getRecentForUser(Long userId) {
        return activityRepository.findTop20ByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<Activity> getForProject(Long projectId) {
        return activityRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
    }
}
