package com.BuildBox.BuildServer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
public class BuildServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(BuildServerApplication.class, args);
	}

}
