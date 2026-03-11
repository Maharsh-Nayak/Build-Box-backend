package com.log_analytics_server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LogAnalyticsServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(LogAnalyticsServerApplication.class, args);
    }

}
