package com.dankposter.dto;

import com.dankposter.model.Meme;
import com.dankposter.model.MemeStatus;

public record MemeDto(
        Long id,
        String externalId,
        String title,
        String imageUrl,
        Double danknessScore,
        boolean posted,
        String description,
        String source,
        boolean liked
) {
    public static MemeDto fromEntity(Meme meme) {
        return new MemeDto(
                meme.getId(),
                meme.getExternalId(),
                meme.getTitle(),
                meme.getImageUrl(),
                meme.getDanknessScore(),
                meme.getStatus() == MemeStatus.POSTED,
                meme.getDescription(),
                meme.getSource() != null ? meme.getSource().name() : null,
                meme.isLiked()
        );
    }
}
