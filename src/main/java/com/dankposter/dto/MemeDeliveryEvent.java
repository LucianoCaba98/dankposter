package com.dankposter.dto;

/**
 * Kafka message payload representing a meme delivery event.
 *
 * @param memeId        database primary key of the meme
 * @param title         meme title
 * @param imageUrl      image URL
 * @param danknessScore dankness score
 */
public record MemeDeliveryEvent(
    Long memeId,
    String title,
    String imageUrl,
    Double danknessScore
) {}
