package com.dankposter.externalIntegrations.d3vd;

import com.dankposter.config.D3vdProperties;
import com.dankposter.dto.d3vd.D3vdResponse;
import com.dankposter.model.Meme;
import com.dankposter.model.MemeSource;
import com.dankposter.model.MemeStatus;
import com.dankposter.model.Source;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
        prefix = "meme.sources",
        name = "d3vd",
        havingValue = "true"
)
public class D3vdMemeSource implements MemeSource {

    private final WebClient d3vdClient;
    private final D3vdProperties d3vdProperties;

    @Override
    public Flux<Meme> fetch() {
        int count = d3vdProperties.getCount();
        log.info("Fetching {} random memes from D3vd API", count);
        return d3vdClient.get()
                .uri("/gimme/{count}", count)
                .retrieve()
                .bodyToMono(D3vdResponse.class)
                .doOnNext(r -> log.info("Fetched {} posts from D3vd", r.getCount()))
                .onErrorResume(e -> {
                    log.warn("Error fetching from D3vd: {}", e.getMessage());
                    return Mono.empty();
                })
                .flatMapMany(resp -> {
                    if (resp == null || resp.getMemes() == null) {
                        return Flux.empty();
                    }
                    return Flux.fromIterable(resp.getMemes());
                })
                .filter(post -> !post.isNsfw() && !post.isSpoiler())
                .filter(post -> post.getUrl() != null && !post.getUrl().isBlank())
                .map(post -> Meme.builder()
                        .externalId("d3vd_" + post.getPostLink().hashCode())
                        .title(post.getTitle() != null && post.getTitle().length() > 500
                                ? post.getTitle().substring(0, 497) + "..."
                                : post.getTitle())
                        .imageUrl(post.getUrl())
                        .description(post.getPostLink())
                        .source(Source.D3VD)
                        .status(MemeStatus.FETCHED)
                        .build());
    }

    @Override
    public String sourceName() {
        return "D3VD";
    }
}
