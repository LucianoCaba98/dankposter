package com.dankposter.service;

import com.dankposter.model.MemeStatus;
import com.dankposter.repository.MemeRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Component
@Slf4j
@ConditionalOnProperty(name = "groq.enabled", havingValue = "true")
public class LikeRescoreTracker {

    private final GroqScoringService groqScoringService;
    private final GroqRateLimiter groqRateLimiter;
    private final MemeRepository memeRepository;
    private final AtomicInteger likeCount = new AtomicInteger(0);

    public LikeRescoreTracker(GroqScoringService groqScoringService,
                              GroqRateLimiter groqRateLimiter,
                              MemeRepository memeRepository) {
        this.groqScoringService = groqScoringService;
        this.groqRateLimiter = groqRateLimiter;
        this.memeRepository = memeRepository;
    }

    public void recordLike() {
        int current = likeCount.incrementAndGet();

        if (!shouldTrigger(current)) {
            return;
        }

        if (!groqRateLimiter.tryAcquire()) {
            log.info("Rate limit reached — skipping like-triggered re-score at like count {}", current);
            return;
        }

        var fetchedMemes = memeRepository.findByStatus(MemeStatus.FETCHED);
        if (fetchedMemes.isEmpty()) {
            log.debug("No FETCHED memes to re-score at like count {}", current);
            return;
        }

        log.info("Like-triggered re-score at like count {} — scoring {} FETCHED memes", current, fetchedMemes.size());
        groqScoringService.score(fetchedMemes).subscribe();
    }

    static boolean shouldTrigger(int count) {
        if (count <= 5) {
            return true;
        }
        if (count <= 15) {
            return count % 5 == 0;
        }
        return count % 10 == 0;
    }

    public int getLikeCount() {
        return likeCount.get();
    }

    public void restore(int count) {
        likeCount.set(count);
        log.info("LikeRescoreTracker restored — like count: {}", count);
    }

    public int toSnapshot() {
        return likeCount.get();
    }
}
