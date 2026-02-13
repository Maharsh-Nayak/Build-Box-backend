package com.BuildBox.BuildServer.util;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.stereotype.Component;

@Component
public class S3Downloader {

        private final S3Client s3Client;

        public S3Downloader(S3Client s3Client) {
                this.s3Client = s3Client;
        }

        public void downloadDirectory(String bucket,
                        String prefix,
                        Path destination) throws IOException {

                ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                                .bucket(bucket)
                                .prefix(prefix)
                                .build();

                ListObjectsV2Response listResponse = s3Client.listObjectsV2(listRequest);

                for (S3Object object : listResponse.contents()) {

                        // Skip folder markers
                        if (object.key().endsWith("/")) {
                                continue;
                        }

                        Path filePath = destination.resolve(
                                        object.key().substring(prefix.length()));

                        Files.createDirectories(filePath.getParent());

                        GetObjectRequest getRequest = GetObjectRequest.builder()
                                        .bucket(bucket)
                                        .key(object.key())
                                        .build();

                        s3Client.getObject(getRequest, filePath);
                }
        }
}
