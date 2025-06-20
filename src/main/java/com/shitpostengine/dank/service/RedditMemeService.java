package com.shitpostengine.dank.service;

import com.shitpostengine.dank.dto.reddit.RedditChild;
import com.shitpostengine.dank.dto.reddit.RedditResponse;
import com.shitpostengine.dank.model.Meme;
import com.shitpostengine.dank.repository.MemeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedditMemeService {

    private final WebClient webClient;
    private final MemeRepository memeRepository;
    private final RedditPostScoringService redditPostScoringService;

    public List<Meme> fetchMemesFromSubreddit(String subreddit, int limit) {
        String url = String.format("https://www.reddit.com/r/%s/hot.json?limit=%d", subreddit, limit);

        RedditResponse response = webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(RedditResponse.class)
                .block();

        if (response == null || response.getData() == null) {
            log.warn("No data received from Reddit for subreddit: {}", subreddit);
            return List.of();
        }

        List<Meme> newMemes = response.getData().getChildren().stream()
                .map(RedditChild::getData)
                .filter(post -> redditPostScoringService.calculateInteractionScore(post) > 0.0)
                .map(post -> Meme.builder()
                        .title(post.getTitle())
                        .imageUrl(post.getUrl())
                        .danknessScore(redditPostScoringService.calculateInteractionScore(post))
                        .description(post.getDescription())
                        .posted(false)
                        .build())
                .filter(meme -> !memeRepository.existsByTitle(meme.getTitle()))
                .collect(Collectors.toList());

        memeRepository.saveAll(newMemes);
        log.info("Saved {} new memes from r/{}", newMemes.size(), subreddit);

        return memeRepository.findAll(Sort.by(Sort.Direction.DESC, "danknessScore"));
    }
}