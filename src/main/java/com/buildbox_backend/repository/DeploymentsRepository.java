package com.buildbox_backend.repository;

import com.buildbox_backend.model.Deployment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeploymentsRepository extends JpaRepository<Deployment, Long> {

}
