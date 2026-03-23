package com.dankposter.service;

import com.dankposter.config.GroqProperties;
import com.dankposter.dto.RateLimiterSnapshot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@ConditionalOnProperty(name = "groq.enabled", havingValue = "true")
public class GroqRateLimiter {

    private final int maxDailyCalls;
    private final int warningThreshold;
    private final AtomicInteger count = new AtomicInteger(0);
    private volatile LocalDate currentDate;

    public GroqRateLimiter(GroqProperties properties) {
        this.maxDailyCalls = properties.getRateLimit().getMaxDailyCalls();
        this.warningThreshold = (int) Math.ceil(maxDailyCalls * 0.8);
        this.currentDate = LocalDate.now(ZoneOffset.UTC);
        log.info("GroqRateLimiter initialized — limit: {}, 80% threshold: {}", maxDailyCalls, warningThreshold);
    }

    /**
     * Attempts to acquire a permit for a Groq API call.
     * Resets the counter if a new UTC day has started.
     *
     * @return true if the call is allowed, false if the daily limit is reached
     */
    public synchronized boolean tryAcquire() {
        resetIfNewDay();

        int current = count.get();

        if (current >= maxDailyCalls) {
            log.warn("Groq daily rate limit reached: {}/{}", current, maxDailyCalls);
            return false;
        }

        int next = count.incrementAndGet();

        if (next >= warningThreshold) {
            log.warn("Groq API usage at {}% — {}/{} daily calls used",
                    (int) ((next * 100.0) / maxDailyCalls), next, maxDailyCalls);
        }

        return true;
    }

    public int getCount() {
        return count.get();
    }

    public LocalDate getDate() {
        return currentDate;
    }

    /**
     * Restores the rate limiter state from a snapshot.
     * If the stored date differs from today (UTC), the counter resets to 0.
     */
    public synchronized void restore(int snapshotCount, LocalDate snapshotDate) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        if (snapshotDate != null && snapshotDate.equals(today)) {
            count.set(snapshotCount);
            currentDate = today;
            log.info("Rate limiter restored — count: {}, date: {}", snapshotCount, today);
        } else {
            count.set(0);
            currentDate = today;
            log.info("Rate limiter reset — snapshot date {} differs from today {}", snapshotDate, today);
        }
    }

    /**
     * Exports the current state as a snapshot for persistence.
     */
    public RateLimiterSnapshot toSnapshot() {
        return new RateLimiterSnapshot(count.get(), currentDate.toString());
    }

    private void resetIfNewDay() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        if (!today.equals(currentDate)) {
            count.set(0);
            currentDate = today;
            log.info("New UTC day detected — rate limiter reset to 0");
        }
    }
}
