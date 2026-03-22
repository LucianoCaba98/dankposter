package com.shitpostengine.dank.service;

import com.shitpostengine.dank.dto.reddit.RedditPost;
import org.springframework.stereotype.Service;

@Service
public class RedditPostScoringService {

    public Double calculateInteractionScore(RedditPost post) {
        String url = post.getUrl();
        String hint = post.getPostHint();

        // Accept direct image URLs and posts Reddit identifies as images
        boolean isImage = false;
        if (url != null) {
            String lower = url.toLowerCase();
            isImage = lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png")
                    || lower.endsWith(".gif") || lower.endsWith(".webp")
                    || lower.contains("i.redd.it") || lower.contains("i.imgur.com")
                    || lower.contains("preview.redd.it");
        }
        if (!isImage && hint != null) {
            isImage = hint.equals("image") || hint.equals("link");
        }

        if (!isImage) {
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
