package com.buildbox_backend.service;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

@Service
public class GitCloneService {

    public File cloneRepository(String link) {
        File cloneFile = new File("/clone"); // Keeping original path logic, though relative path might be better
        if (cloneFile.exists()) {
            cleanup(cloneFile);
            System.out.println("Cleaned up existing directory.");
        }
        cloneFile.mkdir();

        try (Git gitclonner = Git.cloneRepository()
                .setURI(link)
                .setDirectory(cloneFile)
                .call()) {
            System.out.println("Cloned: " + gitclonner.getRepository().getDirectory());
        } catch (GitAPIException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to clone repository", e);
        }
        return cloneFile;
    }

    public void cleanup(File file) {
        try {
            if (file.exists()) {
                 Files.walk(file.toPath())
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to cleanup directory", e);
        }
    }
}
