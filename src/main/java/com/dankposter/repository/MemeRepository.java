package com.dankposter.repository;

import com.dankposter.model.Meme;
import com.dankposter.model.MemeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MemeRepository extends JpaRepository<Meme, Long> {
    boolean existsByExternalId(String externalId);
    Page<Meme> findByStatusOrderByIdDesc(MemeStatus status, Pageable pageable);
    Page<Meme> findAllByOrderByIdDesc(Pageable pageable);
    List<Meme> findByStatus(MemeStatus status);
    long countByStatus(MemeStatus status);
    List<Meme> findByLikedTrue();
}
