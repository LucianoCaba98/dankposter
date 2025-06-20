package com.shitpostengine.dank;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DankApplication {

	public static void main(String[] args) {
		SpringApplication.run(DankApplication.class, args);
	}

}
