package com.buildbox_backend.controller;

import com.buildbox_backend.model.Domain;
import com.buildbox_backend.model.Project;
import com.buildbox_backend.repository.DomainRepository;
import com.buildbox_backend.service.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects/{slug}/domains")
public class DomainController {

    @Autowired
    private DomainRepository domainRepository;

    @Autowired
    private ProjectService projectService;

    @GetMapping
    public ResponseEntity<List<Domain>> listDomains(@PathVariable String slug) {
        Project project = projectService.getBySlug(slug).orElse(null);
        if (project == null)
            return ResponseEntity.notFound().build();
        return ResponseEntity.ok(domainRepository.findByProjectId(project.getId()));
    }

    @PostMapping
    public ResponseEntity<?> addDomain(@PathVariable String slug, @RequestBody Map<String, String> body) {
        Project project = projectService.getBySlug(slug).orElse(null);
        if (project == null)
            return ResponseEntity.notFound().build();

        String domainName = body.get("domain");
        if (domainName == null || domainName.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Domain name is required"));
        }

        if (domainRepository.existsByDomain(domainName)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Domain already in use"));
        }

        Domain domain = new Domain();
        domain.setProject(project);
        domain.setDomain(domainName);
        domain.setVerified(false);
        domain.setCreatedAt(LocalDateTime.now());

        return ResponseEntity.ok(domainRepository.save(domain));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDomain(@PathVariable String slug, @PathVariable Long id) {
        domainRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Domain removed"));
    }
}
