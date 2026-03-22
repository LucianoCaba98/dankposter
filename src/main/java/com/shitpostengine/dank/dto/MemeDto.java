package com.shitpostengine.dank.dto;

import com.shitpostengine.dank.model.Meme;

public record MemeDto(
        Long id,
        String redditId,
        String title,
        String imageUrl,
        Double danknessScore,
        boolean posted,
        String description
) {
    public static MemeDto fromEntity(Meme meme) {
        return new MemeDto(
                meme.getId(),
                meme.getRedditId(),
                meme.getTitle(),
                meme.getImageUrl(),
                meme.getDanknessScore(),
                meme.isPosted(),
                meme.getDescription()
        );
    }
}
