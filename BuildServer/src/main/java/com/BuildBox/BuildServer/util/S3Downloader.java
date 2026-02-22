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

                System.out.println("Downloading directory: " + bucket + "/" + prefix);
                System.out.println("Destination: " + destination);

                ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                                .bucket(bucket)
                                .prefix(prefix)
                                .build();

                System.out.println("Request: " + listRequest);
                System.out.println("Req " + listRequest.bucket() + " " + listRequest.prefix() + " ");

                ListObjectsV2Response listResponse = s3Client.listObjectsV2(listRequest);
                System.out.println("download size : " + listResponse.contents().size());

                for (S3Object object : listResponse.contents()) {

                        // Skip folder markers
                        if (object.key().endsWith("/")) {
                                continue;
                        }

//                        Path filePath = destination.resolve(
//                                        object.key().substring(prefix.length()));

                        String relativeKey = object.key().substring(prefix.length());

                        if (relativeKey.startsWith("/")) {
                                relativeKey = relativeKey.substring(1);
                        }

                        Path filePath = destination.resolve(relativeKey);

                        Files.createDirectories(filePath.getParent());

                        GetObjectRequest getRequest = GetObjectRequest.builder()
                                        .bucket(bucket)
                                        .key(object.key())
                                        .build();

                        System.out.println("Downloading " + filePath);
                        System.out.println("Downloading " + getRequest);

                        try{
                                s3Client.getObject(getRequest, filePath);
                        } catch (Exception e) {
                                throw new RuntimeException(e);
                        }
                }
        }
}
