package com.dankposter.dto;

/**
 * Snapshot of a single meme for persistence to dankposter-state.json.
 */
public record MemeSnapshot(
        Long id,
        String externalId,
        String status,
        String source,
        String title,
        String imageUrl,
        Double danknessScore,
        String description,
        boolean liked
) {}
