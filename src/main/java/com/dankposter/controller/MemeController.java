package com.dankposter.controller;

import com.dankposter.dto.MemeDto;
import com.dankposter.model.MemeStatus;
import com.dankposter.repository.MemeRepository;
import com.dankposter.service.LikeRescoreTracker;
import com.dankposter.service.MemeIntercalator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/memes")
@RequiredArgsConstructor
public class MemeController {

    private final MemeRepository memeRepository;
    private final Optional<LikeRescoreTracker> likeRescoreTracker;

    @GetMapping("/posted")
    public Page<MemeDto> getPostedMemes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return memeRepository.findByStatusOrderByIdDesc(MemeStatus.POSTED, PageRequest.of(page, size))
                .map(MemeDto::fromEntity);
    }

    @GetMapping("/all")
    public Page<MemeDto> getAllMemes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return memeRepository.findAllByOrderByIdDesc(PageRequest.of(page, size))
                .map(MemeDto::fromEntity);
    }

    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        long total = memeRepository.count();
        long posted = memeRepository.countByStatus(MemeStatus.POSTED);
        long fetched = memeRepository.countByStatus(MemeStatus.FETCHED);
        long failed = memeRepository.countByStatus(MemeStatus.FAILED);
        return Map.of(
                "total", total,
                "posted", posted,
                "pending", fetched,
                "failed", failed
        );
    }

    @GetMapping("/queue")
    public List<MemeDto> getPostingQueue() {
        var fetched = memeRepository.findByStatus(MemeStatus.FETCHED);
        var intercalated = MemeIntercalator.intercalate(fetched);
        return intercalated.stream().map(MemeDto::fromEntity).toList();
    }

    @PatchMapping("/{id}/like")
    public ResponseEntity<MemeDto> toggleLike(@PathVariable Long id) {
        return memeRepository.findById(id)
                .map(meme -> {
                    boolean wasLiked = meme.isLiked();
                    meme.setLiked(!wasLiked);
                    var saved = memeRepository.save(meme);
                    if (!wasLiked) {
                        likeRescoreTracker.ifPresent(LikeRescoreTracker::recordLike);
                    }
                    return ResponseEntity.ok(MemeDto.fromEntity(saved));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMeme(@PathVariable Long id) {
        if (memeRepository.existsById(id)) {
            memeRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/failed")
    public ResponseEntity<Map<String, Long>> clearFailed() {
        var failed = memeRepository.findByStatusOrderByIdDesc(MemeStatus.FAILED, PageRequest.of(0, 1000));
        long count = failed.getTotalElements();
        memeRepository.deleteAll(failed.getContent());
        return ResponseEntity.ok(Map.of("deleted", count));
    }
}
