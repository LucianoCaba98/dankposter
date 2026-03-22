package com.shitpostengine.dank.service;

import com.shitpostengine.dank.config.RedditProperties;
import com.shitpostengine.dank.dto.MemeDeliveryEvent;
import com.shitpostengine.dank.dto.MemeMessage;
import com.shitpostengine.dank.dto.reddit.RedditChild;
import com.shitpostengine.dank.dto.reddit.RedditResponse;
import com.shitpostengine.dank.model.Meme;
import com.shitpostengine.dank.repository.MemeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedditFetcherService {

    private final WebClient webClient;
    private final MemeRepository memeRepository;
    private final RedditPostScoringService redditPostScoringService;
    private final RedditProperties redditProperties;
    private final Optional<SqsProducerService> sqsProducerService;
    private final Optional<KafkaProducerService> kafkaProducerService;
    private final MemeEventPublisher memeEventPublisher;

    public void fetchMemesFromSubreddit() {
        List<Meme> allMemes = redditProperties.getSubreddits().stream()
                .flatMap(subreddit -> {
                    try {
                        String url = String.format("https://www.reddit.com/r/%s/hot.json?limit=%d",
                                subreddit.getName(), subreddit.getLimit());

                        RedditResponse response = webClient.get()
                                .uri(URI.create(url))
                                .header("User-Agent", "DankPoster/1.0")
                                .retrieve()
                                .bodyToMono(RedditResponse.class)
                                .block();

                        if (response == null || response.getData() == null) {
                            log.warn("No data from r/{}", subreddit.getName());
                            return Stream.<Meme>of();
                        }

                        return response.getData().getChildren().stream()
                                .map(RedditChild::getData)
                                .filter(post -> redditPostScoringService.calculateInteractionScore(post) > 0.0)
                                .map(post -> Meme.builder()
                                        .redditId(post.getId())
                                        .title(post.getTitle() != null && post.getTitle().length() > 500
                                                ? post.getTitle().substring(0, 497) + "..."
                                                : post.getTitle())
                                        .imageUrl(post.getUrl())
                                        .danknessScore(redditPostScoringService.calculateInteractionScore(post))
                                        .description(post.getDescription())
                                        .posted(false)
                                        .build());
                    } catch (Exception e) {
                        log.warn("Failed to fetch from r/{}: {}", subreddit.getName(), e.getMessage());
                        return Stream.<Meme>of();
                    }
                })
                .distinct()
                .sorted(Comparator.comparingDouble(Meme::getDanknessScore).reversed())
                .toList();

        if (sqsProducerService.isPresent()) {
            // SQS enabled: convert to MemeMessage and send via SQS instead of persisting directly
            allMemes.forEach(meme -> {
                MemeMessage message = new MemeMessage(
                        meme.getTitle(),
                        meme.getImageUrl(),
                        "reddit",
                        meme.getRedditId(),
                        meme.getDanknessScore()
                );
                sqsProducerService.get().send(message);
            });
        } else {
            // SQS disabled: persist directly (existing behavior)
            List<Meme> memesToSave = allMemes.stream()
                    .filter(meme -> !memeRepository.existsByRedditId(meme.getRedditId()))
                    .toList();

            List<Meme> savedMemes = memeRepository.saveAll(memesToSave);
            memeEventPublisher.publishIngested(savedMemes);

            // If Kafka is enabled, publish delivery events for each saved meme
            kafkaProducerService.ifPresent(kafkaService ->
                    savedMemes.forEach(savedMeme -> {
                        MemeDeliveryEvent event = new MemeDeliveryEvent(
                                savedMeme.getId(),
                                savedMeme.getTitle(),
                                savedMeme.getImageUrl(),
                                savedMeme.getDanknessScore()
                        );
                        kafkaService.publishDeliveryEvent(event);
                    })
            );
        }

        log.info("🔥 Scheduler updated memes from {} subreddits", redditProperties.getSubreddits().size());

        memeRepository.findAll(Sort.by(Sort.Direction.DESC, "danknessScore"));
    }
}
