package com.buildbox_backend.service;

import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;

@Service
public class DeployService {

//    @Autowired
//    private DeploymentsRepository deploymentsRepository;

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

//    public boolean findByProjectId(Long projectId) {
//        return deploymentsRepository.existsByProjectId(projectId);
//    }

//    public Deployment save(Long projectId, Project project) {
//
//        Deployment oldDeployment = deploymentsRepository.findByProjectId(projectId);
//
//        Deployment newDeployment = new Deployment();
//
//        if (oldDeployment != null) {
//            newDeployment.setVersion(oldDeployment.getVersion() + 1);
//            newDeployment.setDeploymentUrl(oldDeployment.getDeploymentUrl());
//            newDeployment.setProject(oldDeployment.getProject());
//        }else{
//            newDeployment.setProject(project);
//            newDeployment.setVersion(1);
//            newDeployment.setDeploymentUrl(project.getUser().getId() + "." + projectId + "." + "localhost");
//        }
//
//        newDeployment.setProject(project);
//        return deploymentsRepository.save(newDeployment);
//    }

}
