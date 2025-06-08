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
    public List<Meme> fetchMemes(
            @RequestParam(defaultValue = "ArgentinaBenderStyle") String subreddit,
            @RequestParam(defaultValue = "500") int limit
    ) {
        return redditMemeService.fetchMemesFromSubreddit(subreddit, limit);
    }
}