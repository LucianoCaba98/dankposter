package com.shitpostengine.dank.service;

import com.shitpostengine.dank.config.DiscordConfig;
import com.shitpostengine.dank.model.Meme;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class DiscordPosterService {

    private final DiscordConfig discordConfig;

    public Mono<Meme> post(Meme meme) {

        WebClient webClient = WebClient.builder()
                .baseUrl("https://discord.com/api")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bot " + discordConfig.getBotToken())
                .build();

        String message = String.format("🔥 **%s**\n%s", meme.getTitle(), meme.getImageUrl());

        return webClient.post()
                .uri("/channels/{channelId}/messages", discordConfig.getChannelId())
                .bodyValue(new DiscordMessage(message))
                .retrieve()
                .bodyToMono(Void.class)
                .thenReturn(meme);

    }

    private record DiscordMessage(String content) {}
}
