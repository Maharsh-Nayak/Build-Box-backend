package com.buildbox_backend.repository;

import com.buildbox_backend.model.EnvVariable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EnvVariableRepository extends JpaRepository<EnvVariable, Long> {

    List<EnvVariable> findByProjectId(Long projectId);

    List<EnvVariable> findByProjectIdAndEnvironment(Long projectId, String environment);
}
