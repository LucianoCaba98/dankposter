package com.shitpostengine.dank.repository;

import com.shitpostengine.dank.model.Meme;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemeRepository extends JpaRepository<Meme, Long> {
}
