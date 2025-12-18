package com.buildbox_backend.controller;

import com.buildbox_backend.service.UploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;

@RestController
@RequestMapping("/upload")
public class UploadController {

    private final UploadService uploadService;

    @Autowired
    public UploadController(UploadService uploadService) {
        this.uploadService = uploadService;
    }

    @PostMapping
    public String upload(@RequestParam String path, @RequestParam String projectId) {
        File folder = new File(path);
        if (!folder.exists() || !folder.isDirectory()) {
            return "Invalid directory";
        }
        uploadService.uploadDirectory(folder, projectId);
        return "Upload initiated";
    }
}
