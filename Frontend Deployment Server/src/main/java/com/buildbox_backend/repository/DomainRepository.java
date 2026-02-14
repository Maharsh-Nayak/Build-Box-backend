package com.buildbox_backend.repository;

import com.buildbox_backend.model.Domain;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DomainRepository extends JpaRepository<Domain, Long> {

    List<Domain> findByProjectId(Long projectId);

    Optional<Domain> findByDomain(String domain);

    boolean existsByDomain(String domain);
}
