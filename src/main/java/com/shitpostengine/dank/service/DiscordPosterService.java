package com.shitpostengine.dank.service;

import com.shitpostengine.dank.config.DiscordConfig;
import com.shitpostengine.dank.model.Meme;
import com.shitpostengine.dank.repository.MemeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DiscordPosterService {

    private final MemeRepository memeRepository;
    private final DiscordConfig discordConfig;

    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://discord.com/api")
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();

    public void postNextUnpostedMeme() {
        Optional<Meme> memeOpt = memeRepository.findFirstByPostedFalseOrderByDanknessScoreDesc();

        if (memeOpt.isEmpty()) {
            log.info("No unposted memes found.");
            return;
        }

        Meme meme = memeOpt.get();
        String message = String.format("🔥 **%s**\n%s", meme.getTitle(), meme.getImageUrl());

        webClient.post()
                .uri("/channels/{channelId}/messages", discordConfig.getChannelId())
                .headers(headers -> headers.setBearerAuth(discordConfig.getBotToken()))
                .bodyValue(new DiscordMessage(message))
                .retrieve()
                .bodyToMono(String.class)
                .doOnSuccess(response -> {
                    log.info("Successfully posted meme to Discord.");
                    meme.setPosted(true);
                    memeRepository.save(meme);
                })
                .doOnError(error -> log.error("Failed to post meme to Discord", error))
                .subscribe();
    }

    private record DiscordMessage(String content) {}
}
