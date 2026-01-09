package com.buildbox_backend.service;

import com.buildbox_backend.model.Deployment;
import com.buildbox_backend.repository.DeploymentsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;

@Service
public class DeployService {

    @Autowired
    private DeploymentsRepository deploymentsRepository;

    public void buildProject(File projectDir) {
        String npmCommand = System.getProperty("os.name").toLowerCase().contains("win")
                ? "npm.cmd"
                : "npm";

        // npm install
        executeCommand(projectDir, npmCommand, "install");
        System.out.println("Executed npm install");

        // npm run build
        executeCommand(projectDir, npmCommand, "run", "build", "--", "--base=./");
        System.out.println("Executed build command");
    }

    private void executeCommand(File dir, String... command) {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(dir);
        try {
            Process process = pb.start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("Command execution failed with exit code: " + exitCode);
            }
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException("Command execution interrupted or failed", e);
        }
    }

    public void saveProject(String userId, String projectName, String link) {
        Deployment deployment = new Deployment();
        deployment.setBranch("main");
        deployment.setDeploymentUrl(link);
        deployment.setCreatedAt(LocalDateTime.now());
        // Note to self : The DB needs a few changes, will update it and complete this part
    }
}
