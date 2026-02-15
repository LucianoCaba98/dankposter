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

//        return webClient.post()
//                .uri("/channels/{channelId}/messages", discordConfig.getChannelId())
//                .bodyValue(new DiscordMessage(message))
//                .retrieve()
//                .onStatus(
//                        status -> status.value() == 429,
//                        response -> {
//                            Duration retryAfter = response.headers()
//                                    .header("Retry-After")
//                                    .stream()
//                                    .findFirst()
//                                    .map(Long::parseLong)
//                                    .map(Duration::ofMillis)
//                                    .orElse(Duration.ofSeconds(5));
//
//                            log.warn("Discord rate limited. Retry after {}", retryAfter);
//                            return Mono.error(new DiscordRateLimitException(retryAfter));
//                        }
//                )
//                .bodyToMono(Map.class)
//                .doOnNext(body -> log.info("Discord response: {}", body))
//                .thenReturn(meme)
//                .onErrorResume(DiscordRateLimitException.class, ex -> {
//                    log.warn("Retrying after {}", ex.getRetryAfter());
//                    return Mono.delay(ex.getRetryAfter()).then(post(meme));
//                })
//
//                .retryWhen(
//                        Retry.from(retrySignals ->
//                                    retrySignals.flatMap(signal -> {
//                                        Throwable error = signal.failure();
//                                        if (error instanceof DiscordRateLimitException rateLimit) {
//                                            return Mono.delay(rateLimit.getRetryAfter());
//                                        }
//                                        return Mono.error(error);
//                                    })
//                                )
//                )
//                .thenReturn(meme);



    private record DiscordMessage(String content) {}
}
