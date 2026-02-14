package com.BuildBox.BuildServer.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ecs.EcsClient;
import software.amazon.awssdk.services.elasticloadbalancingv2.ElasticLoadBalancingV2Client;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class AwsConfig {

        // S3 Configuration
        @Value("${aws.s3.accessKey}")
        private String s3AccessKey;

        @Value("${aws.s3.secretKey}")
        private String s3SecretKey;

        // ECS Configuration
        @Value("${aws.ecs.accessKey}")
        private String ecsAccessKey;

        @Value("${aws.ecs.secretKey}")
        private String ecsSecretKey;

        @Value("${aws.region}")
        private String region;

        @Bean
        public S3Client s3Client() {
                return S3Client.builder()
                                .region(Region.of(region))
                                .credentialsProvider(StaticCredentialsProvider.create(
                                                AwsBasicCredentials.create(s3AccessKey, s3SecretKey)))
                                .build();
        }

        @Bean
        public EcsClient ecsClient() {
                return EcsClient.builder()
                                .region(Region.of(region))
                                .credentialsProvider(StaticCredentialsProvider.create(
                                                AwsBasicCredentials.create(ecsAccessKey, ecsSecretKey)))
                                .build();
        }

        @Bean
        public Ec2Client ec2Client() {
                return Ec2Client.builder()
                                .region(Region.of(region))
                                .credentialsProvider(StaticCredentialsProvider.create(
                                                AwsBasicCredentials.create(ecsAccessKey, ecsSecretKey)))
                                .build();
        }

        @Bean
        public CloudWatchLogsClient cloudWatchLogsClient() {
                return CloudWatchLogsClient.builder()
                                .region(Region.of(region))
                                .credentialsProvider(StaticCredentialsProvider.create(
                                                AwsBasicCredentials.create(ecsAccessKey, ecsSecretKey)))
                                .build();
        }

        @Bean
        public software.amazon.awssdk.services.ecr.EcrClient ecrClient() {
                return software.amazon.awssdk.services.ecr.EcrClient.builder()
                                .region(Region.of(region))
                                .credentialsProvider(StaticCredentialsProvider.create(
                                                AwsBasicCredentials.create(ecsAccessKey, ecsSecretKey)))
                                .build();
        }

        @Bean
        @ConditionalOnProperty(name = "routing.backend", havingValue = "alb")
        public ElasticLoadBalancingV2Client elasticLoadBalancingV2Client() {
                return ElasticLoadBalancingV2Client.builder()
                                .region(Region.of(region))
                                .credentialsProvider(StaticCredentialsProvider.create(
                                                AwsBasicCredentials.create(ecsAccessKey, ecsSecretKey)))
                                .build();
        }
}
