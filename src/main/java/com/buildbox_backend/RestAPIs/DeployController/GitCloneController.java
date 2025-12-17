package com.buildbox_backend.RestAPIs.DeployController;

import com.buildbox_backend.RequestBodies.CloneRequests.GitCloneRequest;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

@RestController
public class GitCloneController {

    @GetMapping("/upload")
    public String check(){
        return "Hey";
    }

    @Autowired
    UploadController uploadController;

    @PostMapping("/upload")
    public String upload(@RequestBody GitCloneRequest request){

        System.out.println("Starting upload");
        String link = request.getLink();
        System.out.println(link);

        // Cloning the repo using JGit

        File cloneFile = new File("/clone");
        if (cloneFile.exists()) {
            // Delete recursively
            try {
                Files.walk(cloneFile.toPath())
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            System.out.println("Cleaned up existing directory.");
        }

        cloneFile.mkdir();

        try (Git gitclonner = Git.cloneRepository()
                .setURI(link)
                .setDirectory(cloneFile)
                .call()) {

            System.out.println("Cloned: " + gitclonner.getRepository().getDirectory());
        }catch (GitAPIException e){
            e.printStackTrace();
        }

        System.out.println("Finished Cloning");

        //Once Clone we will install node modules

        String npmCommand = System.getProperty("os.name").toLowerCase().contains("win")
                ? "npm.cmd"
                : "npm";

        ProcessBuilder pb = new ProcessBuilder(npmCommand, "install");
        pb.directory(cloneFile);
        try {
            pb.start().waitFor();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        System.out.println("Executed npm command");


        //Now we will build it
        ProcessBuilder buildPb = new ProcessBuilder(npmCommand, "run", "build");
        buildPb.directory(cloneFile);
        try {
            buildPb.start().waitFor();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        System.out.println("Executed build command");

        // Upload the files
        String bucketUrl=null;

        File distFolder = new File(cloneFile, "dist");

        uploadController.uploadDirectory(distFolder,"demo");


        //Delete the clone once Uploaded

        try {
            Files.walk(cloneFile.toPath())
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("Cleaned up existing directory.");


        return bucketUrl;

    }

}
