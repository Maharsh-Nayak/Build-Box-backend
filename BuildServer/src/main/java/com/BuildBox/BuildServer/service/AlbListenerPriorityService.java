package com.BuildBox.BuildServer.service;

import com.BuildBox.BuildServer.model.AlbListenerRule;
import com.BuildBox.BuildServer.model.Project;
import com.BuildBox.BuildServer.repository.AlbListenerRuleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Service to allocate deterministic, collision-free priorities for ALB listener
 * rules.
 * Priorities are stored in the database to ensure they remain persistent and
 * unique across redeploys.
 */
@Service
public class AlbListenerPriorityService {

    private final AlbListenerRuleRepository repository;

    public AlbListenerPriorityService(AlbListenerRuleRepository repository) {
        this.repository = repository;
    }

    /**
     * Allocates a priority for a project on a specific listener.
     * Uses a deterministic base priority + incremental collision resolution.
     */
    @Transactional
    public int allocatePriority(String listenerArn, Project project) {
        // 1. Check if a priority is already assigned
        Optional<AlbListenerRule> existing = repository.findByListenerArnAndProject(listenerArn, project);
        if (existing.isPresent()) {
            return existing.get().getPriority();
        }

        // 2. Deterministic base priority calculation
        // Algorithm: 1000 + (abs(hash(projectSlug)) % 10000)
        int basePriority = 1000 + (Math.abs(project.getSlug().hashCode()) % 10000);
        int attempt = basePriority;

        // 3. Collision resolution loop
        // We track priorities in our own DB to avoid expensive AWS API calls for every
        // attempt.
        // Hashing alone is unsafe because hashes can collide; DB persistence ensures
        // finality.
        while (repository.findByListenerArnAndPriority(listenerArn, attempt).isPresent()) {
            System.out
                    .println("⚠️ Priority collision for " + project.getSlug() + " at " + attempt + ". Incrementing...");
            attempt++;
        }

        // 4. Persist the allocation
        AlbListenerRule newRule = new AlbListenerRule();
        newRule.setProject(project);
        newRule.setListenerArn(listenerArn);
        newRule.setPriority(attempt);
        repository.save(newRule);

        System.out.println("✅ Allocated ALB priority " + attempt + " for project: " + project.getSlug());
        return attempt;
    }
}
