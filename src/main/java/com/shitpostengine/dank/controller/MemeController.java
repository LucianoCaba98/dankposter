package com.shitpostengine.dank.controller;

import com.shitpostengine.dank.dto.MemeDto;
import com.shitpostengine.dank.repository.MemeRepository;
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
        return memeRepository.findByPostedTrueOrderByIdDesc(PageRequest.of(page, size))
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
