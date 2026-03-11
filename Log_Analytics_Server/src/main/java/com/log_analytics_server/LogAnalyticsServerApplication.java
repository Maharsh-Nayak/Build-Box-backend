package com.log_analytics_server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

@SpringBootApplication
@EnableScheduling
@EnableR2dbcRepositories(basePackages = "com.log_analytics_server.Repository")
public class LogAnalyticsServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(LogAnalyticsServerApplication.class, args);
    }

}
