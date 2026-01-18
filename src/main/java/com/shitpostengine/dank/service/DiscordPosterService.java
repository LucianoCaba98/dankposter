package com.shitpostengine.dank.service;

import com.shitpostengine.dank.config.DiscordConfig;
import com.shitpostengine.dank.model.Meme;
import com.shitpostengine.dank.model.error.DiscordRateLimitException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

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
                .onStatus(
                        status -> status.value() == 429,
                        response -> {
                            Duration retryAfter = response.headers()
                                    .header("Retry-After")
                                    .stream()
                                    .findFirst()
                                    .map(Long::parseLong)
                                    .map(Duration::ofMillis)
                                    .orElse(Duration.ofSeconds(5));

                            log.warn("Discord rate limited. Retry after {}", retryAfter);
                            return Mono.error(new DiscordRateLimitException(retryAfter));
                        }
                )
                .bodyToMono(Void.class)
                .thenReturn(meme)
                .onErrorResume(DiscordRateLimitException.class, ex -> {
                    log.warn("Retrying after {}", ex.getRetryAfter());
                    return Mono.delay(ex.getRetryAfter()).then(post(meme));
                })

                .retryWhen(
                        Retry.from(retrySignals ->
                                    retrySignals.flatMap(signal -> {
                                        Throwable error = signal.failure();
                                        if (error instanceof DiscordRateLimitException rateLimit) {
                                            return Mono.delay(rateLimit.getRetryAfter());
                                        }
                                        return Mono.error(error);
                                    })
                                )
                )
                .thenReturn(meme);

    }

    private record DiscordMessage(String content) {}
}
