package com.BuildBox.BuildServer.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ecs.EcsClient;
import software.amazon.awssdk.services.elasticloadbalancingv2.ElasticLoadBalancingV2Client;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class AwsConfig {

        // Optional explicit keys — if blank, falls back to DefaultCredentialsProvider
        // (AWS CLI profile locally, IAM Role on EC2)
        @Value("${aws.s3.accessKey:}")
        private String s3AccessKey;

        @Value("${aws.s3.secretKey:}")
        private String s3SecretKey;

        @Value("${aws.ecs.accessKey:}")
        private String ecsAccessKey;

        @Value("${aws.ecs.secretKey:}")
        private String ecsSecretKey;

        @Value("${aws.region}")
        private String region;

        /**
         * Returns StaticCredentialsProvider if explicit keys are set,
         * otherwise falls back to DefaultCredentialsProvider
         * (AWS CLI ~/.aws/credentials locally, EC2 Instance Metadata on AWS).
         */
        private AwsCredentialsProvider resolveCredentials(String accessKey, String secretKey) {
                if (accessKey != null && !accessKey.isBlank()
                                && secretKey != null && !secretKey.isBlank()) {
                        return StaticCredentialsProvider.create(
                                        AwsBasicCredentials.create(accessKey, secretKey));
                }
                return DefaultCredentialsProvider.create();
        }

        @Bean
        public S3Client s3Client() {
                return S3Client.builder()
                                .region(Region.of(region))
                                .credentialsProvider(resolveCredentials(s3AccessKey, s3SecretKey))
                                .build();
        }

        @Bean
        public EcsClient ecsClient() {
                return EcsClient.builder()
                                .region(Region.of(region))
                                .credentialsProvider(resolveCredentials(ecsAccessKey, ecsSecretKey))
                                .build();
        }

        @Bean
        public Ec2Client ec2Client() {
                return Ec2Client.builder()
                                .region(Region.of(region))
                                .credentialsProvider(resolveCredentials(ecsAccessKey, ecsSecretKey))
                                .build();
        }

        @Bean
        public CloudWatchLogsClient cloudWatchLogsClient() {
                return CloudWatchLogsClient.builder()
                                .region(Region.of(region))
                                .credentialsProvider(resolveCredentials(ecsAccessKey, ecsSecretKey))
                                .build();
        }

        @Bean
        public software.amazon.awssdk.services.ecr.EcrClient ecrClient() {
                return software.amazon.awssdk.services.ecr.EcrClient.builder()
                                .region(Region.of(region))
                                .credentialsProvider(resolveCredentials(ecsAccessKey, ecsSecretKey))
                                .build();
        }

        @Bean
        @ConditionalOnProperty(name = "routing.backend", havingValue = "alb")
        public ElasticLoadBalancingV2Client elasticLoadBalancingV2Client() {
                return ElasticLoadBalancingV2Client.builder()
                                .region(Region.of(region))
                                .credentialsProvider(resolveCredentials(ecsAccessKey, ecsSecretKey))
                                .build();
        }
}
