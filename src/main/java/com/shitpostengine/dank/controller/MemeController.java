package com.shitpostengine.dank.controller;

import com.shitpostengine.dank.model.Meme;
import com.shitpostengine.dank.service.MemeService;
import com.shitpostengine.dank.service.RedditMemeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/memes")
@RequiredArgsConstructor
public class MemeController {

    private final RedditMemeService redditMemeService;

    @PostMapping("/fetch")
    public List<Meme> fetchMemes() {
        return redditMemeService.fetchMemesFromSubreddit();
    }
}