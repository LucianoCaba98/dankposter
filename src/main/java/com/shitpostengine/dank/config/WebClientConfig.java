package com.shitpostengine.dank.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient webClient() {
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))  // 16 MB
                .build();

        return WebClient.builder()
                .defaultHeader(HttpHeaders.USER_AGENT,
                        "DankPosterBot/1.0 (by u/LucianoCaba98)")
                .exchangeStrategies(strategies)
                .build();
    }
}
