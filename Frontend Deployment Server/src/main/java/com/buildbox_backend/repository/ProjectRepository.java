package com.buildbox_backend.repository;

import com.buildbox_backend.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    Optional<Project> findBySlug(String slug);

    List<Project> findByUserId(Long userId);

    List<Project> findByTeamId(Long teamId);

    boolean existsBySlug(String slug);
}
