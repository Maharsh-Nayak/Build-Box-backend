package com.buildbox_backend.controller;

import com.buildbox_backend.dto.GitCloneRequest;
import com.buildbox_backend.service.GitCloneService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/git")
public class GitCloneController {

    private final GitCloneService gitCloneService;

    @Autowired
    public GitCloneController(GitCloneService gitCloneService) {
        this.gitCloneService = gitCloneService;
    }

    @PostMapping("/clone")
    public String cloneRepository(@RequestBody GitCloneRequest request) {
        gitCloneService.cloneRepository(request.getLink());
        return "Repository cloned successfully";
    }
}
