package com.shitpostengine.dank.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "memes",
        uniqueConstraints = @UniqueConstraint(columnNames = "reddit_id")
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Meme {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reddit_id", nullable = false, unique = true)
    private String redditId;
    @Enumerated(EnumType.STRING)
    private MemeStatus status;

    private String title;
    private String imageUrl;
    private Double danknessScore;
    @Lob
    private String description;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Meme meme = (Meme) o;
        return title != null && title.equals(meme.title);
    }

    @Override
    public int hashCode() {
        return title != null ? title.hashCode() : 0;
    }
}