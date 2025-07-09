package com.shitpostengine.dank.service;

import com.shitpostengine.dank.config.RedditProperties;
import com.shitpostengine.dank.dto.reddit.RedditChild;
import com.shitpostengine.dank.dto.reddit.RedditResponse;
import com.shitpostengine.dank.model.Meme;
import com.shitpostengine.dank.repository.MemeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedditFetcherService {

    private final WebClient webClient;
    private final MemeRepository memeRepository;
    private final RedditPostScoringService redditPostScoringService;
    private final RedditProperties redditProperties;

    public void fetchMemesFromSubreddit() {
        List<Meme> allMemes = redditProperties.getSubreddits().stream()
                .flatMap(subreddit -> {
                    String url = String.format("https://www.reddit.com/r/%s/hot.json?limit=%d",
                            subreddit.getName(), subreddit.getLimit());

                    RedditResponse response = webClient.get()
                            .uri(url)
                            .retrieve()
                            .bodyToMono(RedditResponse.class)
                            .block();

                    if (response == null || response.getData() == null) {
                        log.warn("No data from r/{}", subreddit.getName());
                        return Stream.of();
                    }


                    return response.getData().getChildren().stream()
                            .map(RedditChild::getData)
                            .filter(post -> redditPostScoringService.calculateInteractionScore(post) > 0.0)
                            .map(post -> Meme.builder()
                                    .redditId(post.getId())
                                    .title(post.getTitle())
                                    .imageUrl(post.getUrl())
                                    .danknessScore(redditPostScoringService.calculateInteractionScore(post))
                                    .description(post.getDescription())
                                    .posted(false)
                                    .build());
                })
                .distinct()
                .sorted(Comparator.comparingDouble(Meme::getDanknessScore).reversed())
                .toList();

        List<Meme> memesToSave = allMemes.stream()
                .filter(meme -> !memeRepository.existsByRedditId(meme.getRedditId()))
                .toList();

        memeRepository.saveAll(memesToSave);

        log.info("🔥 Scheduler updated memes from {} subreddits", redditProperties.getSubreddits().size());

        memeRepository.findAll(Sort.by(Sort.Direction.DESC, "danknessScore"));
    }
}
