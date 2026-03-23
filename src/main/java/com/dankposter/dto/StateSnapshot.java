package com.dankposter.dto;

import java.util.List;

/**
 * Top-level snapshot structure for dankposter-state.json.
 * Contains all memes, rate limiter state, and like counter.
 */
public record StateSnapshot(
        List<MemeSnapshot> memes,
        RateLimiterSnapshot rateLimiter,
        int likeCount
) {}
