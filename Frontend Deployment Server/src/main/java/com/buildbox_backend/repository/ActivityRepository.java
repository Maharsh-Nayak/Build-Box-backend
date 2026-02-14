package com.buildbox_backend.repository;

import com.buildbox_backend.model.Activity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActivityRepository extends JpaRepository<Activity, Long> {

    List<Activity> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Activity> findByProjectIdOrderByCreatedAtDesc(Long projectId);

    List<Activity> findTop20ByUserIdOrderByCreatedAtDesc(Long userId);
}
