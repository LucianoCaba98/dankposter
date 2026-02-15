package com.dankposter.service;

import com.dankposter.config.DiscordConfig;
import com.dankposter.externalIntegrations.discord.dto.DiscordMessagePayload;
import com.dankposter.externalIntegrations.discord.render.MemeRenderService;
import com.dankposter.model.Meme;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class DiscordPosterService {

    private final DiscordConfig discordConfig;
    private final WebClient discordClient;
    private final MemeRenderService memeRenderService;


    public Mono<Meme> post(Meme meme) {
        DiscordMessagePayload payload = memeRenderService.render(meme);

        log.info("Discord payload: {}", payload);


        return discordClient.post()
                .uri("/channels/{channelId}/messages", discordConfig.getChannelId())
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(String.class)
                .thenReturn(meme);
    }

    private record DiscordMessage(String content) {}
}
