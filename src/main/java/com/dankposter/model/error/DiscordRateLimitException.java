package com.dankposter.model.error;

import lombok.Getter;

import java.time.Duration;

@Getter
public class DiscordRateLimitException extends RuntimeException {
    private final Duration retryAfter;

    public DiscordRateLimitException(Duration retryAfter) {
        this.retryAfter = retryAfter;
    }

}
