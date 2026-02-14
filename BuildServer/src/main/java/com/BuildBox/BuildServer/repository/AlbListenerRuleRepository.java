package com.BuildBox.BuildServer.repository;

import com.BuildBox.BuildServer.model.AlbListenerRule;
import com.BuildBox.BuildServer.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AlbListenerRuleRepository extends JpaRepository<AlbListenerRule, Long> {
    Optional<AlbListenerRule> findByListenerArnAndProject(String listenerArn, Project project);

    Optional<AlbListenerRule> findByListenerArnAndPriority(String listenerArn, Integer priority);
}
