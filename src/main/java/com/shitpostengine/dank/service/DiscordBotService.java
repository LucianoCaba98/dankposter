package com.shitpostengine.dank.service;

import com.shitpostengine.dank.config.DiscordConfig;
import com.shitpostengine.dank.model.Meme;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@RequiredArgsConstructor
@Slf4j
public class DiscordBotService {

    private final DiscordConfig discordConfig;

    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://discord.com/api")
            .defaultHeader("Authorization", "Bot " + discordConfig.getBotToken())
            .build();

    public void sendMeme(Meme meme) {
        String channelId = discordConfig.getChannelId();
        String content = "**" + meme.getTitle() + "**\n" + meme.getImageUrl();

        webClient.post()
                .uri("/channels/{channelId}/messages", channelId)
                .bodyValue(new DiscordMessage(content))
                .retrieve()
                .bodyToMono(String.class)
                .doOnError(error -> log.error("Error sending meme to Discord", error))
                .subscribe(response -> log.info("Meme posted to Discord: {}", response));
    }

    record DiscordMessage(String content) {}
}
