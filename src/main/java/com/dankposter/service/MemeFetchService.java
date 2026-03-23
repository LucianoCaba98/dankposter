package com.dankposter.service;

import com.dankposter.model.Meme;
import com.dankposter.model.MemeSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import jakarta.annotation.PostConstruct;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MemeFetchService {

    private final List<MemeSource> sources;

    @PostConstruct
    void logSources() {
        log.info("Registered meme sources: {}", sources.stream().map(MemeSource::sourceName).toList());
    }

    public Flux<Meme> fetch() {
        return Flux.fromIterable(sources)
                .flatMap(source -> {
                    log.info("Fetching from source: {}", source.sourceName());
                    return source.fetch();
                });
    }
}
