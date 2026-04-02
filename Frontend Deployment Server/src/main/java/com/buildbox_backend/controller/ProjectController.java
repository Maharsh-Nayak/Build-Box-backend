package com.buildbox_backend.controller;

import com.buildbox_backend.model.Project;
import com.buildbox_backend.model.User;
import com.buildbox_backend.repository.UserRepository;
import com.buildbox_backend.service.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/projects")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class ProjectController {

    @Autowired
    private ProjectService projectService;

    @Autowired
    private UserRepository userRepository;

    @PostMapping
    public ResponseEntity<?> createProject(@RequestBody Map<String, String> body, Authentication auth) {
        User user = getUser(auth);
        if (user == null)
            return ResponseEntity.status(401).build();

        String name = body.get("name");
        String repoUrl = body.get("repoUrl");
        String basePath = body.get("basePath");

        if (name == null || name.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Project name is required"));
        }

        Project project = projectService.createProject(name, repoUrl, basePath, user);
        return ResponseEntity.ok(project);
    }

    @GetMapping
    public ResponseEntity<List<Project>> listProjects(Authentication auth) {
        User user = getUser(auth);
        System.out.println("user: " + user);

        if (user == null)
            return ResponseEntity.status(401).build();

        List<Project> ans = projectService.getUserProjects(user.getId());
//        System.out.println(ans.get(0).getName());
        return ResponseEntity.ok(projectService.getUserProjects(user.getId()));
    }

    @GetMapping("/{slug}")
    public ResponseEntity<?> getProject(@PathVariable("slug") String slug, Authentication auth) {

        System.out.println("Slug: " + slug);

        return projectService.getBySlug(slug)
                .map(p -> ResponseEntity.ok((Object) p))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{slug}/owner")
    public ResponseEntity<?> getProjectOwner(@PathVariable("slug") String slug) {
        return projectService.getBySlug(slug)
                .map(project -> {
                    User owner = project.getUser();
                    if (owner != null) {
                        return ResponseEntity.ok(Map.of(
                            "userId", owner.getId(),
                            "email", owner.getEmail(),
                            "name", owner.getName()
                        ));
                    } else {
                        return ResponseEntity.ok(Map.of("userId", "unknown"));
                    }
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/by-name/{projectName}/owner")
    public ResponseEntity<?> getProjectOwnerByName(@PathVariable("projectName") String projectName) {
        Project project = projectService.getByName(projectName);
        if (project != null) {
            User owner = project.getUser();
            if (owner != null) {
                return ResponseEntity.ok(Map.of(
                    "userId", owner.getId(),
                    "email", owner.getEmail(),
                    "name", owner.getName()
                ));
            } else {
                return ResponseEntity.ok(Map.of("userId", "unknown"));
            }
        }
        return ResponseEntity.notFound().build();
    }

    @PutMapping("/{slug}")
    public ResponseEntity<?> updateProject(@PathVariable("slug") String slug,
            @RequestBody Map<String, String> body,
            Authentication auth) {
        return projectService.getBySlug(slug)
                .map(project -> {
                    Project updated = projectService.updateProject(
                            project, body.get("name"), body.get("repoUrl"));
                    return ResponseEntity.ok((Object) updated);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{slug}")
    public ResponseEntity<?> deleteProject(@PathVariable("slug") String slug, Authentication auth) {
        return projectService.getBySlug(slug)
                .map(project -> {
                    projectService.deleteProject(project.getId());
                    return ResponseEntity.ok(Map.of("message", "Deleted"));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/test/seed")
    public ResponseEntity<?> seed() {
        User user = userRepository.findByEmail("test@example.com").orElseGet(() -> {
            User newUser = new User();
            newUser.setEmail("test@example.com");
            newUser.setName("Test User");
            return userRepository.save(newUser);
        });

        if (!projectService.getBySlug("test-fullstack-app").isPresent()) {
            Project p = projectService.createProject("Test Fullstack App",
                    "https://github.com/kavyacp123/build-box-test.git", "Backend", user);
            projectService.updateProject(p, p.getName(), p.getRepoUrl());
        }

        return ResponseEntity.ok(Map.of("message", "Test project seeded", "slug", "test-fullstack-app"));
    }

    private User getUser(Authentication auth) {
        if (auth == null)
            return null;
        System.out.println(auth.getName());
        Optional<User> u = userRepository.findByEmail(auth.getName());

        u.ifPresent(user -> System.out.println(user.getName()));

        return u.orElse(null);
    }
}
