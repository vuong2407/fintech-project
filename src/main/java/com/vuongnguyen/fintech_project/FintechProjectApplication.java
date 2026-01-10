package com.vuongnguyen.fintech_project;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FintechProjectApplication {

	public static void main(String[] args) {
		SpringApplication.run(FintechProjectApplication.class, args);
	}

}
