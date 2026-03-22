package com.shitpostengine.dank.dto;

/**
 * SQS message payload representing a meme to be persisted.
 *
 * @param title             meme title
 * @param imageUrl          image URL
 * @param sourceIdentifier  source name, e.g. "reddit"
 * @param sourceSpecificId  source-specific ID, e.g. Reddit post ID
 * @param danknessScore     pre-computed dankness score
 */
public record MemeMessage(
    String title,
    String imageUrl,
    String sourceIdentifier,
    String sourceSpecificId,
    Double danknessScore
) {}
