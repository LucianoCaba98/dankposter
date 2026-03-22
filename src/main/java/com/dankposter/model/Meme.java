package com.dankposter.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "memes",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "external_id"),
                @UniqueConstraint(columnNames = "image_url")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Meme {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id", nullable = false, unique = true)
    private String externalId;
    @Enumerated(EnumType.STRING)
    private MemeStatus status;

    @Enumerated(EnumType.STRING)
    private Source source;

    @Column(length = 500)
    private String title;
    @Column(name = "image_url", length = 2048, unique = true)
    private String imageUrl;
    private Double danknessScore;
    @Lob
    private String description;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Meme meme)) return false;
        return externalId != null && externalId.equals(meme.externalId);
    }

    @Override
    public int hashCode() {
        return externalId != null ? externalId.hashCode() : 0;
    }
}
