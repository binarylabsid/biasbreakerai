package com.binarylabsid.biasbreaker_ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BiasbreakerAiApplication {

	public static void main(String[] args) {
		SpringApplication.run(BiasbreakerAiApplication.class, args);
	}

}
