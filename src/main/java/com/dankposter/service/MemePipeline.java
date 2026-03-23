package com.dankposter.service;

import com.dankposter.config.KafkaProperties;
import com.dankposter.dto.MemeDeliveryEvent;
import com.dankposter.model.Meme;
import com.dankposter.model.MemeStatus;
import com.dankposter.repository.MemeRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.List;

@Service
@Slf4j
public class MemePipeline {

    private final MemeFetchService memeFetchService;
    private final DiscordPosterService discordPosterService;
    private final MemeRepository memeRepository;
    private final MemeEventPublisher memeEventPublisher;
    private final KafkaProducerService kafkaProducerService;
    private final KafkaProperties kafkaProperties;

    public MemePipeline(MemeFetchService memeFetchService,
                        DiscordPosterService discordPosterService,
                        MemeRepository memeRepository,
                        MemeEventPublisher memeEventPublisher,
                        KafkaProducerService kafkaProducerService,
                        KafkaProperties kafkaProperties) {
        this.memeFetchService = memeFetchService;
        this.discordPosterService = discordPosterService;
        this.memeRepository = memeRepository;
        this.memeEventPublisher = memeEventPublisher;
        this.kafkaProducerService = kafkaProducerService;
        this.kafkaProperties = kafkaProperties;
    }

    private void publishToKafka(Meme meme) {
        var event = new MemeDeliveryEvent(
                meme.getId(),
                meme.getTitle(),
                meme.getImageUrl(),
                meme.getDanknessScore()
        );
        kafkaProducerService.publishDeliveryEvent(event);
        log.info("Published meme to Kafka: id={}, title={}", meme.getId(), meme.getTitle());
    }

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        log.info("Kafka delivery — memes routed through Kafka");

        // Stage 1: Fetch and persist memes as fast as possible
        Flux.interval(Duration.ZERO, Duration.ofMinutes(30))
                .onBackpressureDrop(tick -> log.debug("Skipping fetch tick {} — previous cycle still running", tick))
                .concatMap(tick -> memeFetchService.fetch()
                        .concatMap(meme ->
                                Mono.fromCallable(() -> memeRepository.save(meme))
                                        .subscribeOn(Schedulers.boundedElastic())
                                        .doOnNext(saved -> {
                                            log.info("Saved meme: id={}, source={}, externalId={}", saved.getId(), saved.getSource(), saved.getExternalId());
                                            memeEventPublisher.publishIngested(List.of(saved));
                                        })
                                        .onErrorResume(DataIntegrityViolationException.class, e -> {
                                            log.debug("Duplicate!: {}", meme.getExternalId());
                                            return Mono.empty();
                                        })
                        )
                        .filter(meme -> meme.getStatus() == MemeStatus.FETCHED)
                        .collectList()
                        .map(MemeIntercalator::intercalate)
                )
                .doOnNext(batch -> log.info("Fetch cycle complete — saved {} new memes", batch.size()))
                .flatMapIterable(batch -> batch)
                .concatMap(meme ->
                        Mono.delay(Duration.ofSeconds(20))
                                .then(Mono.fromRunnable(() -> publishToKafka(meme))
                                        .subscribeOn(Schedulers.boundedElastic())
                                        .thenReturn(meme))
                                .onErrorResume(e -> {
                                    log.error("Failed publishing meme {} to Kafka",
                                            meme.getExternalId(), e);
                                    return Mono.empty();
                                })
                )
                .subscribe(
                        success -> log.info("Processed meme {}", success.getExternalId()),
                        error -> log.error("Pipeline error", error)
                );
    }
}
