package com.shitpostengine.dank.controller;

import com.shitpostengine.dank.model.Meme;
import com.shitpostengine.dank.service.RedditMemeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/reddit")
public class RedditController {

    private final RedditMemeService redditMemeService;

    public RedditController(RedditMemeService redditMemeService) {
        this.redditMemeService = redditMemeService;
    }

    @GetMapping("/fetch")
    public List<Meme> fetchMemes() {
        return redditMemeService.fetchMemesFromSubreddit("dankmemes", 10);
    }
}
