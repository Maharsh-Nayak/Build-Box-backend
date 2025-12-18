package com.buildbox_backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.File;
import java.nio.file.Path;

@Service
public class UploadService {

    private final S3Client s3client;
    private final String bucketName = "buildbox-frontend";

    @Autowired
    public UploadService(S3Client s3client) {
        this.s3client = s3client;
        System.out.println("Got S3Client");
    }

    public void uploadDirectory(File folder, String projectId) {
        if (!folder.exists() || !folder.isDirectory()) {
            System.out.println("Invalid folder: " + folder.getAbsolutePath());
            return;
        }

        Path basePath = folder.toPath();
        uploadRecursive(folder, basePath, projectId);
        System.out.println("Done uploading files");
    }

    private void uploadRecursive(File current, Path basePath, String projectId) {
        File[] files = current.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                uploadRecursive(file, basePath, projectId);
            } else {
                Path relative = basePath.relativize(file.toPath());
                String s3Key = projectId + "/" + relative.toString().replace("\\", "/");

                try {
                    s3client.putObject(
                            PutObjectRequest.builder()
                                    .bucket(bucketName)
                                    .key(s3Key)
                                    .contentType(determineContentType(file.getName()))
                                    .build(),
                            file.toPath()
                    );

                    System.out.println("Uploaded: " + s3Key);

                } catch (Exception e) {
                    System.out.println("Failed: " + s3Key);
                    e.printStackTrace();
                }
            }
        }
    }

    private String determineContentType(String fileName) {
        if (fileName.endsWith(".html")) return "text/html";
        if (fileName.endsWith(".js")) return "application/javascript";
        if (fileName.endsWith(".css")) return "text/css";
        if (fileName.endsWith(".json")) return "application/json";
        if (fileName.endsWith(".png")) return "image/png";
        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) return "image/jpeg";
        if (fileName.endsWith(".svg")) return "image/svg+xml";
        return "application/octet-stream";
    }
}
