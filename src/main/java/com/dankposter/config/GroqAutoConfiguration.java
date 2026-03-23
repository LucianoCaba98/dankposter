package com.dankposter.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Configuration
@ConditionalOnProperty(name = "groq.enabled", havingValue = "true")
@EnableConfigurationProperties(GroqProperties.class)
@RequiredArgsConstructor
public class GroqAutoConfiguration {

    private final GroqProperties groqProperties;

    @Bean
    public WebClient groqClient() {
        log.info("Configuring Groq WebClient with model: {}", groqProperties.getModel());
        return WebClient.builder()
                .baseUrl("https://api.groq.com/openai/v1")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + groqProperties.getApi().getKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
