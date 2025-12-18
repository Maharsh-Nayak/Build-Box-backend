package com.buildbox_backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class S3Config {

    @Bean
    public S3Client getS3Client() {

        String accessKey = "";
        String secretKey = "";
        AwsCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);

        return S3Client.builder().region(Region.AP_SOUTH_1).credentialsProvider(StaticCredentialsProvider.create(credentials)).build();
    }

}
