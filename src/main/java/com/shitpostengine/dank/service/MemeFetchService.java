package com.shitpostengine.dank.service;

import com.shitpostengine.dank.model.Meme;
import com.shitpostengine.dank.model.MemeSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MemeFetchService {

    private final List<MemeSource> sources;

    public Flux<Meme> fetch() {
        return Flux.fromIterable(sources)
                .flatMap(source -> {
                    log.info("Fetching from source: {}", source.sourceName());
                    return source.fetch();
                });
    }
}
