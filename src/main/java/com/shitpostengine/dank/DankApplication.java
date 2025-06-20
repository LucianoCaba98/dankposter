package com.shitpostengine.dank;

import com.shitpostengine.dank.config.DiscordConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
//@EnableConfigurationProperties(DiscordConfig.class)
public class DankApplication {

	public static void main(String[] args) {
		SpringApplication.run(DankApplication.class, args);
	}

}
