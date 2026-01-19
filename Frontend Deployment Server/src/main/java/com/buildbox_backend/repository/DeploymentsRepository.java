package com.buildbox_backend.repository;

import com.buildbox_backend.model.Deployment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeploymentsRepository extends JpaRepository<Deployment, Long> {
    public boolean existsByProjectId(Long projectId);
    public Deployment findByProjectId(Long projectId);
}
