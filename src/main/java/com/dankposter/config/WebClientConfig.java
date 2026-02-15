package com.dankposter.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

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
                .defaultHeader(HttpHeaders.ACCEPT, "application/json")
                .defaultHeader(HttpHeaders.ACCEPT_ENCODING, "gzip")
                .exchangeStrategies(strategies)
                .build();
    }

    @Bean
    public WebClient giphyClient(
            @Value("${giphy.api.key}") String apiKey) {

        return WebClient.builder()
                .baseUrl("https://api.giphy.com")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .filter((req, next) -> {
                    URI uri = UriComponentsBuilder.fromUri(req.url())
                            .queryParam("api_key", apiKey)
                            .build(true)
                            .toUri();

                    return next.exchange(
                            ClientRequest.from(req).url(uri).build()
                    );
                })
                .build();
    }

    @Bean
    public WebClient discordClient(DiscordConfig discordConfig) {
        return WebClient.builder()
                .baseUrl("https://discord.com/api/v10")
                .defaultHeader(HttpHeaders.USER_AGENT,
                        "DankPosterBot/1.0 (by u/LucianoCaba98)")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bot " + discordConfig.getBotToken())
                .build();
    }
}
