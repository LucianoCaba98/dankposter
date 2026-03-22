package com.dankposter;

import com.dankposter.config.DiscordConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties(DiscordConfig.class)
@EnableScheduling
public class DankApplication {

	public static void main(String[] args) {
		SpringApplication.run(DankApplication.class, args);
	}
}
