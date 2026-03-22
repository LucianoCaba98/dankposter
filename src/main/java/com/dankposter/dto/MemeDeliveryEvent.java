package com.dankposter.dto;

public record MemeDeliveryEvent(
    Long memeId,
    String title,
    String imageUrl,
    Double danknessScore
) {}
