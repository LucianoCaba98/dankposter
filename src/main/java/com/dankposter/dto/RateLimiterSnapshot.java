package com.dankposter.dto;

/**
 * Snapshot of the rate limiter state for persistence.
 *
 * @param count number of API calls made on the given date
 * @param date  the UTC date in ISO format (yyyy-MM-dd)
 */
public record RateLimiterSnapshot(int count, String date) {
}
