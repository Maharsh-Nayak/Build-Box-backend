package com.buildbox_backend.service;

import com.buildbox_backend.model.Project;
import com.buildbox_backend.model.User;
import com.buildbox_backend.repository.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ProjectService {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ActivityService activityService;

    public Project createProject(String name, String repoUrl, String basePath, User user) {
        Project project = new Project();
        project.setName(name);
        String slug = generateSlug(name);
        project.setSlug(slug);
        project.setRepoUrl(repoUrl);
        project.setBasePath(basePath);
        project.setUser(user);
        project.setCreatedAt(LocalDateTime.now());

        project.setDeploy_url("https://buildbox-frontend.s3.ap-south-1.amazonaws.com/"+user.getId()+"/"+slug+"/Frontend/index.html");
        project.setBackend_link("api."+slug+".localhost:8000");

        Project saved = projectRepository.save(project);
        activityService.log(user.getId(), saved.getId(), "Created project: " + name);
        return saved;
    }

    public List<Project> getUserProjects(Long userId) {
        return projectRepository.findByUserId(userId);
    }

    public List<Project> getTeamProjects(Long teamId) {
        return projectRepository.findByTeamId(teamId);
    }

    public Optional<Project> getBySlug(String slug) {

        Optional<Project>project = (projectRepository.findBySlug(slug));

        if (project.isPresent()) {
            System.out.println(project.get().getName());
        }else{
            System.out.println("Project not found");
        }

        return project;
    }

    public Project getByName(String name) {
        return projectRepository.findByName(name);
    }

    public Optional<Project> getById(Long id) {
        return projectRepository.findById(id);
    }

    public Project updateProject(Project project, String name, String repoUrl) {
        if (name != null)
            project.setName(name);
        if (repoUrl != null)
            project.setRepoUrl(repoUrl);
        return projectRepository.save(project);
    }

    public void deleteProject(Long id) {
        projectRepository.deleteById(id);
    }

    private String generateSlug(String name) {
        String base = name.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");

        String slug = base;
        int counter = 1;
        while (projectRepository.existsBySlug(slug)) {
            slug = base + "-" + counter++;
        }
        return slug;
    }
}
