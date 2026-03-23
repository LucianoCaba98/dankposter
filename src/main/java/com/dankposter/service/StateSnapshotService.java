package com.dankposter.service;

import com.dankposter.dto.MemeSnapshot;
import com.dankposter.dto.RateLimiterSnapshot;
import com.dankposter.dto.StateSnapshot;
import com.dankposter.model.Meme;
import com.dankposter.model.MemeStatus;
import com.dankposter.model.Source;
import com.dankposter.repository.MemeRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@ConditionalOnProperty(name = "dankposter.snapshot.enabled", havingValue = "true", matchIfMissing = true)
public class StateSnapshotService {

    private static final String SNAPSHOT_FILE = "dankposter-state.json";

    private final MemeRepository memeRepository;
    private final Optional<GroqRateLimiter> groqRateLimiter;
    private final Optional<LikeRescoreTracker> likeRescoreTracker;
    private final ObjectMapper objectMapper;

    public StateSnapshotService(MemeRepository memeRepository,
                                Optional<GroqRateLimiter> groqRateLimiter,
                                Optional<LikeRescoreTracker> likeRescoreTracker) {
        this.memeRepository = memeRepository;
        this.groqRateLimiter = groqRateLimiter;
        this.likeRescoreTracker = likeRescoreTracker;
        this.objectMapper = new ObjectMapper();
    }

    @EventListener(ApplicationReadyEvent.class)
    @Order(0)
    public void loadSnapshot() {
        File file = new File(SNAPSHOT_FILE);
        if (!file.exists()) {
            log.info("No snapshot file found at {} — starting with empty state", SNAPSHOT_FILE);
            return;
        }

        try {
            StateSnapshot snapshot = objectMapper.readValue(file, StateSnapshot.class);

            // Restore memes with dedup
            if (snapshot.memes() != null) {
                int restored = 0;
                for (MemeSnapshot ms : snapshot.memes()) {
                    if (memeRepository.existsByExternalId(ms.externalId())) {
                        log.debug("Skipping duplicate meme from snapshot: {}", ms.externalId());
                        continue;
                    }
                    try {
                        Meme meme = Meme.builder()
                                .externalId(ms.externalId())
                                .status(MemeStatus.valueOf(ms.status()))
                                .source(Source.valueOf(ms.source()))
                                .title(ms.title())
                                .imageUrl(ms.imageUrl())
                                .danknessScore(ms.danknessScore())
                                .description(ms.description())
                                .liked(ms.liked())
                                .build();
                        memeRepository.save(meme);
                        restored++;
                    } catch (DataIntegrityViolationException e) {
                        log.debug("Duplicate meme on snapshot load (imageUrl conflict): {}", ms.externalId());
                    } catch (IllegalArgumentException e) {
                        log.warn("Invalid enum value in snapshot for meme {}: {}", ms.externalId(), e.getMessage());
                    }
                }
                log.info("Restored {} memes from snapshot", restored);
            }

            // Restore rate limiter
            groqRateLimiter.ifPresent(rl -> {
                if (snapshot.rateLimiter() != null) {
                    LocalDate date = LocalDate.parse(snapshot.rateLimiter().date());
                    rl.restore(snapshot.rateLimiter().count(), date);
                }
            });

            // Restore like counter
            likeRescoreTracker.ifPresent(lt -> lt.restore(snapshot.likeCount()));

            log.info("Snapshot loaded successfully from {}", SNAPSHOT_FILE);
        } catch (Exception e) {
            log.warn("Failed to read snapshot file {} — starting with empty state: {}", SNAPSHOT_FILE, e.getMessage());
        }
    }

    @PreDestroy
    public void writeSnapshot() {
        try {
            List<Meme> allMemes = memeRepository.findAll();
            List<MemeSnapshot> memeSnapshots = allMemes.stream()
                    .map(m -> new MemeSnapshot(
                            m.getId(),
                            m.getExternalId(),
                            m.getStatus().name(),
                            m.getSource().name(),
                            m.getTitle(),
                            m.getImageUrl(),
                            m.getDanknessScore(),
                            m.getDescription(),
                            m.isLiked()
                    ))
                    .toList();

            RateLimiterSnapshot rlSnapshot = groqRateLimiter
                    .map(GroqRateLimiter::toSnapshot)
                    .orElse(new RateLimiterSnapshot(0, LocalDate.now().toString()));

            int likeCount = likeRescoreTracker
                    .map(LikeRescoreTracker::toSnapshot)
                    .orElse(0);

            StateSnapshot snapshot = new StateSnapshot(memeSnapshots, rlSnapshot, likeCount);
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(SNAPSHOT_FILE), snapshot);
            log.info("Snapshot written to {} — {} memes, rate limiter count: {}, like count: {}",
                    SNAPSHOT_FILE, memeSnapshots.size(), rlSnapshot.count(), likeCount);
        } catch (Exception e) {
            log.error("Failed to write snapshot to {}: {}", SNAPSHOT_FILE, e.getMessage());
        }
    }
}
