package com.buildbox_backend.service;

import com.buildbox_backend.model.Project;
import com.buildbox_backend.model.User;
import com.buildbox_backend.repository.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ProjectService {

    private ProjectRepository projectRepository;

    @Autowired
    public ProjectService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    public boolean findsByUserId(Long userId) {
        return projectRepository.existByUserId(userId);
    }

    public Project save(String projectName, User user, String url) {

        Project project = new Project();
        project.setName(projectName);
        project.setCreatedAt(LocalDateTime.now());
        project.setRepoUrl(url);
        project.setUser(user);

        return projectRepository.save(project);

    }

}
