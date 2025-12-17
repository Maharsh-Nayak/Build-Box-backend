package com.buildbox_backend.RestAPIs.DeployController;

import jakarta.websocket.server.ServerEndpoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.File;

@Service
public class UploadController {

    private S3Client s3client;
    private final String bucketName = "buildbox-frontend";

    @Autowired
    public UploadController(S3Client s3client) {
        this.s3client = s3client;
        System.out.println("Got S3Client");
    }

    public void uploadDirectory(File folder, String projectId) {
        File[] files = folder.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                uploadDirectory(file, projectId + "/" + file.getName());
            } else {
                String s3Key = projectId + "/" + file.getName();
                s3client.putObject(PutObjectRequest.builder()
                                .bucket(bucketName)
                                .key(s3Key)
                                .contentType(determineContentType(file.getName()))
                                .build(),
                        file.toPath());
            }
        }

        System.out.println("Done uploading files");
    }

    private String determineContentType(String fileName) {
        if (fileName.endsWith(".html")) return "text/html";
        if (fileName.endsWith(".js")) return "application/javascript";
        if (fileName.endsWith(".css")) return "text/css";
        return "application/octet-stream";
    }

}
