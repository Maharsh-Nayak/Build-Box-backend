package com.buildbox_backend.controller;

import com.buildbox_backend.model.EnvVariable;
import com.buildbox_backend.model.Project;
import com.buildbox_backend.repository.EnvVariableRepository;
import com.buildbox_backend.service.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects/{slug}/env")
public class EnvVariableController {

    @Autowired
    private EnvVariableRepository envVariableRepository;

    @Autowired
    private ProjectService projectService;

    @GetMapping
    public ResponseEntity<List<EnvVariable>> listEnvVars(@PathVariable("slug") String slug,
            @RequestParam(name = "environment", required = false) String environment) {
        Project project = projectService.getBySlug(slug).orElse(null);
        if (project == null)
            return ResponseEntity.notFound().build();

        if (environment != null) {
            return ResponseEntity.ok(
                    envVariableRepository.findByProjectIdAndEnvironment(project.getId(), environment));
        }
        return ResponseEntity.ok(envVariableRepository.findByProjectId(project.getId()));
    }

    @PostMapping
    public ResponseEntity<?> createEnvVar(@PathVariable("slug") String slug, @RequestBody Map<String, String> body) {
        Project project = projectService.getBySlug(slug).orElse(null);
        if (project == null)
            return ResponseEntity.notFound().build();

        String name = body.get("name");
        String value = body.get("value");
        String environment = body.get("environment");

        if (name == null || value == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "name and value are required"));
        }

        EnvVariable env = new EnvVariable();
        env.setProject(project);
        env.setName(name);
        env.setValue(value);
        env.setEnvironment(environment != null ? environment : "PRODUCTION");
        env.setCreatedAt(LocalDateTime.now());

        return ResponseEntity.ok(envVariableRepository.save(env));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateEnvVar(@PathVariable("slug") String slug,
            @PathVariable("id") Long id,
            @RequestBody Map<String, String> body) {
        return envVariableRepository.findById(id)
                .map(env -> {
                    if (body.get("value") != null)
                        env.setValue(body.get("value"));
                    if (body.get("name") != null)
                        env.setName(body.get("name"));
                    if (body.get("environment") != null)
                        env.setEnvironment(body.get("environment"));
                    return ResponseEntity.ok((Object) envVariableRepository.save(env));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteEnvVar(@PathVariable("slug") String slug, @PathVariable("id") Long id) {
        envVariableRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Environment variable removed"));
    }
}
