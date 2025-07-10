package com.shitpostengine.dank.service;

import com.shitpostengine.dank.dto.reddit.RedditPost;
import org.springframework.stereotype.Service;

@Service
public class RedditPostScoringService {

    public Double calculateInteractionScore(RedditPost post) {
        String url = post.getUrl();

        if (url == null || !(url.endsWith(".jpg") || url.endsWith(".jpeg") || url.endsWith(".png"))) {
            return 0.0;
        }

        double upvoteWeight = 1.0;
        double commentWeight = 2.0;
        double awardWeight = 3.0;

        double score = post.getUpvotes() * upvoteWeight +
                post.getCommentCount() * commentWeight +
                post.getAwardCount() * awardWeight;

        if (score < 0) {
            return 0.0;
        }

        return score;
    }

    public boolean isCategoryAllowed(RedditPost post, String... allowedFlairs) {
        if (allowedFlairs == null || allowedFlairs.length == 0) return true;
        if (post.getDescription() == null) return false;

        for (String flair : allowedFlairs) {
            if (post.getDescription().equalsIgnoreCase(flair)) {
                return true;
            }
        }
        return false;
    }
}
