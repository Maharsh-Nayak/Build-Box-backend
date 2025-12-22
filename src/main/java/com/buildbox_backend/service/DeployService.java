package com.buildbox_backend.service;

import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;

@Service
public class DeployService {

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
}
