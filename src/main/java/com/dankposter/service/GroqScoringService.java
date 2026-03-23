package com.dankposter.service;

import com.dankposter.config.GroqProperties;
import com.dankposter.dto.groq.GroqChatRequest;
import com.dankposter.dto.groq.GroqMessage;
import com.dankposter.dto.groq.GroqScoreEntry;
import com.dankposter.model.Meme;
import com.dankposter.repository.MemeRepository;
import lombok.RequiredArgsConstructor;
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
public class GroqScoringService {

    private final WebClient groqClient;
    private final GroqProperties groqProperties;
    private final MemeRepository memeRepository;
    private final GroqRateLimiter groqRateLimiter;

    public GroqScoringService(@Qualifier("groqClient") WebClient groqClient,
                              GroqProperties groqProperties,
                              MemeRepository memeRepository,
                              GroqRateLimiter groqRateLimiter) {
        this.groqClient = groqClient;
        this.groqProperties = groqProperties;
        this.memeRepository = memeRepository;
        this.groqRateLimiter = groqRateLimiter;
    }

    public Mono<List<Meme>> score(List<Meme> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return Mono.just(candidates);
        }

        if (!groqRateLimiter.tryAcquire()) {
            log.info("Groq rate limit reached — skipping scoring for {} candidates", candidates.size());
            return Mono.just(candidates);
        }

        return Mono.fromCallable(() -> memeRepository.findByLikedTrue())
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(likedMemes -> {
                    if (likedMemes.isEmpty()) {
                        log.debug("No liked memes found — skipping scoring");
                        return Mono.just(candidates);
                    }

                    String systemMessage = buildSystemMessage(likedMemes);
                    String userMessage = buildUserMessage(candidates);

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
                                List<GroqScoreEntry> scores = GroqResponseParser.parseScores(json);

                                Map<String, Double> scoreMap = scores.stream()
                                        .collect(Collectors.toMap(
                                                e -> e.title().toLowerCase(),
                                                GroqScoreEntry::score,
                                                (a, b) -> b
                                        ));

                                candidates.forEach(meme -> {
                                    if (meme.getTitle() != null) {
                                        Double score = scoreMap.get(meme.getTitle().toLowerCase());
                                        if (score != null) {
                                            meme.setDanknessScore(score);
                                        } else {
                                            log.debug("No score returned for meme: {}", meme.getTitle());
                                        }
                                    }
                                });

                                return Mono.fromCallable(() -> memeRepository.saveAll(candidates))
                                        .subscribeOn(Schedulers.boundedElastic());
                            });
                })
                .onErrorResume(e -> {
                    log.error("Error during Groq scoring — returning candidates unchanged: {}", e.getMessage(), e);
                    return Mono.just(candidates);
                });
    }

    private String buildSystemMessage(List<Meme> likedMemes) {
        String likedList = IntStream.range(0, likedMemes.size())
                .mapToObj(i -> (i + 1) + ". " + likedMemes.get(i).getTitle())
                .collect(Collectors.joining("\n"));

        return "You are a meme ranking assistant. The user has liked the following memes, which represent their taste:\n"
                + likedList
                + "\n\nScore each of the following candidate memes on a scale of 0.0 to 1.0 based on how well they match the user's taste. "
                + "Return ONLY a JSON array of objects with \"title\" and \"score\" fields. No explanation.";
    }

    private String buildUserMessage(List<Meme> candidates) {
        return IntStream.range(0, candidates.size())
                .mapToObj(i -> (i + 1) + ". " + candidates.get(i).getTitle())
                .collect(Collectors.joining("\n"));
    }
}
