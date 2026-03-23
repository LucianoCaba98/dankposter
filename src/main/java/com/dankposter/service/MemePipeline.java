package com.dankposter.service;

import com.dankposter.config.KafkaProperties;
import com.dankposter.dto.MemeDeliveryEvent;
import com.dankposter.model.Meme;
import com.dankposter.model.MemeStatus;
import com.dankposter.repository.MemeRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class MemePipeline {

    private final MemeFetchService memeFetchService;
    private final DiscordPosterService discordPosterService;
    private final MemeRepository memeRepository;
    private final MemeEventPublisher memeEventPublisher;
    private final KafkaProducerService kafkaProducerService;
    private final KafkaProperties kafkaProperties;
    private final Optional<GroqScoringService> groqScoringService;
    private final Optional<GroqDescriptionService> groqDescriptionService;

    public MemePipeline(MemeFetchService memeFetchService,
                        DiscordPosterService discordPosterService,
                        MemeRepository memeRepository,
                        MemeEventPublisher memeEventPublisher,
                        KafkaProducerService kafkaProducerService,
                        KafkaProperties kafkaProperties,
                        Optional<GroqScoringService> groqScoringService,
                        Optional<GroqDescriptionService> groqDescriptionService) {
        this.memeFetchService = memeFetchService;
        this.discordPosterService = discordPosterService;
        this.memeRepository = memeRepository;
        this.memeEventPublisher = memeEventPublisher;
        this.kafkaProducerService = kafkaProducerService;
        this.kafkaProperties = kafkaProperties;
        this.groqScoringService = groqScoringService;
        this.groqDescriptionService = groqDescriptionService;
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
    @Order(1)
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
                        .flatMap(batch -> {
                            Mono<List<Meme>> result = Mono.just(batch);
                            if (groqScoringService.isPresent()) {
                                result = result.flatMap(groqScoringService.get()::score);
                            }
                            if (groqDescriptionService.isPresent()) {
                                result = result.flatMap(groqDescriptionService.get()::generateDescriptions);
                            }
                            return result.doOnNext(b -> b.sort(Comparator.comparing(
                                    Meme::getDanknessScore,
                                    Comparator.nullsLast(Comparator.reverseOrder()))));
                        })
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
