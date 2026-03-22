package com.dankposter.controller;

import com.dankposter.dto.MemeDto;
import com.dankposter.model.MemeStatus;
import com.dankposter.repository.MemeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/memes")
@RequiredArgsConstructor
public class MemeController {

    private final MemeRepository memeRepository;

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
}
