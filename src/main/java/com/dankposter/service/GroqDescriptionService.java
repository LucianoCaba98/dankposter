package com.dankposter.service;

import com.dankposter.config.GroqProperties;
import com.dankposter.dto.groq.GroqChatRequest;
import com.dankposter.dto.groq.GroqDescriptionEntry;
import com.dankposter.dto.groq.GroqMessage;
import com.dankposter.model.Meme;
import com.dankposter.repository.MemeRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@Slf4j
@ConditionalOnProperty(name = "groq.enabled", havingValue = "true")
public class GroqDescriptionService {

    private final WebClient groqClient;
    private final GroqProperties groqProperties;
    private final MemeRepository memeRepository;
    private final GroqRateLimiter groqRateLimiter;

    public GroqDescriptionService(@Qualifier("groqClient") WebClient groqClient,
                                  GroqProperties groqProperties,
                                  MemeRepository memeRepository,
                                  GroqRateLimiter groqRateLimiter) {
        this.groqClient = groqClient;
        this.groqProperties = groqProperties;
        this.memeRepository = memeRepository;
        this.groqRateLimiter = groqRateLimiter;
    }

    public Mono<List<Meme>> generateDescriptions(List<Meme> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return Mono.just(candidates);
        }

        List<Meme> needsDescription = candidates.stream()
                .filter(m -> m.getDescription() == null || m.getDescription().isBlank())
                .toList();

        if (needsDescription.isEmpty()) {
            log.debug("All candidates already have descriptions — skipping generation");
            return Mono.just(candidates);
        }

        if (!groqRateLimiter.tryAcquire()) {
            log.info("Groq rate limit reached — skipping description generation for {} candidates", needsDescription.size());
            return Mono.just(candidates);
        }

        String systemMessage = "You are a meme description writer. For each meme below, write a concise 1-3 sentence description "
                + "based on the title, source, and image URL. Return ONLY a JSON array of objects with \"title\" and \"description\" fields.";

        String userMessage = buildUserMessage(needsDescription);

        GroqChatRequest request = new GroqChatRequest(
                groqProperties.getModel(),
                List.of(
                        new GroqMessage("system", systemMessage),
                        new GroqMessage("user", userMessage)
                ),
                groqProperties.getMaxTokens()
        );

        return groqClient.post()
                .uri("/chat/completions")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(String.class)
                .flatMap(responseBody -> {
                    String content = GroqResponseParser.extractContent(responseBody);
                    String json = GroqResponseParser.stripMarkdownFences(content);
                    List<GroqDescriptionEntry> descriptions = GroqResponseParser.parseDescriptions(json);

                    Map<String, String> descriptionMap = descriptions.stream()
                            .collect(Collectors.toMap(
                                    e -> e.title().toLowerCase(),
                                    GroqDescriptionEntry::description,
                                    (a, b) -> b
                            ));

                    needsDescription.forEach(meme -> {
                        if (meme.getTitle() != null) {
                            String desc = descriptionMap.get(meme.getTitle().toLowerCase());
                            if (desc != null) {
                                meme.setDescription(desc);
                            } else {
                                log.debug("No description returned for meme: {}", meme.getTitle());
                            }
                        }
                    });

                    return Mono.fromCallable(() -> memeRepository.saveAll(candidates))
                            .subscribeOn(Schedulers.boundedElastic())
                            .thenReturn(candidates);
                })
                .onErrorResume(e -> {
                    log.error("Error during Groq description generation — returning candidates unchanged: {}", e.getMessage(), e);
                    return Mono.just(candidates);
                });
    }

    private String buildUserMessage(List<Meme> memes) {
        return IntStream.range(0, memes.size())
                .mapToObj(i -> {
                    Meme m = memes.get(i);
                    return (i + 1) + ". Title: \"" + m.getTitle() + "\", Source: " + m.getSource() + ", URL: " + m.getImageUrl();
                })
                .collect(Collectors.joining("\n"));
    }
}
