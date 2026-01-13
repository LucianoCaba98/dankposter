package com.shitpostengine.dank.service;

import com.shitpostengine.dank.config.RedditProperties;
import com.shitpostengine.dank.dto.reddit.RedditChild;
import com.shitpostengine.dank.dto.reddit.RedditResponse;
import com.shitpostengine.dank.model.Meme;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedditFetcherService {

    private final WebClient webClient;
    private final RedditProperties redditProperties;

    public Flux<Meme> fetch() {
        log.info("fetching memes #hard");
        return Flux.fromIterable(redditProperties.getSubreddits())

                .flatMap(source -> {
                    String url = String.format("https://www.reddit.com/r/%s/hot.json?limit=%d", source.getName(), source.getLimit());
                    return webClient
                            .get()
                            .uri(url)
                            .retrieve()
                            .bodyToMono(RedditResponse.class)
                            .onErrorResume(e -> {
                                log.warn("Error fetching r/{}", source.getName(), e);
                                return Mono.empty();
                            });
                }, 2)
                .flatMap(response -> Flux.fromIterable(response.getData().getChildren()))
                .map(RedditChild::getData)
                .map(meme -> Meme.builder()
                        .redditId(meme.getId())
                        .description(meme.getDescription())
                        .title(meme.getTitle())
                        .imageUrl(meme.getUrl())
                        .build());
    }
}
