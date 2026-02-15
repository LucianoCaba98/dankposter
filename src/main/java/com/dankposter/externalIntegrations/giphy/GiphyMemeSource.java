package com.dankposter.externalIntegrations.giphy;

import com.dankposter.dto.giphy.GiphyGif;
import com.dankposter.dto.giphy.GiphyResponse;
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

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
        prefix = "meme.sources",
        name = "giphy",
        havingValue = "true"
)
public class GiphyMemeSource implements MemeSource {

    private final WebClient giphyClient;

    @Override
    public Flux<Meme> fetch() {
        log.info("fetching memes from giphy #hellyeah");
        return giphyClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/gifs/trending")
                        .queryParam("limit", 25)
                        .queryParam("rating", "pg")
                        .queryParam("lang", "es")
                        .build())
                .retrieve()
                .bodyToMono(GiphyResponse.class)
                .flatMapMany(resp -> Flux.fromIterable(resp.getData()))
                .map(this::toMeme);
    }

    @Override
    public String sourceName() {
        return "GIPHY";
    }

    private Meme toMeme(GiphyGif gif) {
        String gifUrl = null;

        if (gif.getImages().getDownsized() != null) {
            gifUrl = gif.getImages().getDownsized().getUrl();
        } else if (gif.getImages().getOriginal() != null) {
            gifUrl = gif.getImages().getOriginal().getUrl();
        } else if (gif.getImages().getFixed_height() != null) {
            gifUrl = gif.getImages().getFixed_height().getUrl();
        }

        if (gifUrl == null) {
            log.warn("No usable gif found for giphy id {}", gif.getId());
            return null;
        }

        return Meme.builder()
                .externalId("giphy_" + gif.getId())
                .title(gif.getTitle())
                .imageUrl(gifUrl) // 🔥 GIF REAL
                .description(gif.getUrl())
                .status(MemeStatus.FETCHED)
                .source(Source.GIPHY)
                .build();
    }

}
