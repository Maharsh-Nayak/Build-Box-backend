package com.buildbox_backend.repository;

import com.buildbox_backend.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    public boolean existByUserId(Long userId);
}
