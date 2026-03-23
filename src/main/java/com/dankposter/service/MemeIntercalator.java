package com.dankposter.service;

import com.dankposter.model.Meme;
import com.dankposter.model.Source;

import java.util.*;

/**
 * Stateless utility that intercalates memes by alternating between Source groups
 * in round-robin order. Preserves original order within each source group.
 */
public final class MemeIntercalator {

    private MemeIntercalator() {}

    /**
     * Intercalates memes by alternating between Source groups in enum-declared order
     * (GIPHY, REDDIT). Preserves original order within each source group.
     * Appends remaining memes from the larger group after alternation is exhausted.
     *
     * @param memes the list of memes to intercalate
     * @return a new list with memes intercalated by source
     */
    public static List<Meme> intercalate(List<Meme> memes) {
        if (memes == null || memes.size() <= 1) {
            return memes == null ? List.of() : new ArrayList<>(memes);
        }

        // Partition into queues preserving insertion order, keyed by Source
        Map<Source, Queue<Meme>> buckets = new LinkedHashMap<>();
        for (Source s : Source.values()) {
            buckets.put(s, new LinkedList<>());
        }
        for (Meme meme : memes) {
            buckets.get(meme.getSource()).add(meme);
        }

        // Round-robin: iterate through sources in enum order, polling one from each non-empty queue
        List<Source> sourceOrder = List.of(Source.values());
        List<Meme> result = new ArrayList<>(memes.size());

        boolean anyPolled = true;
        while (anyPolled) {
            anyPolled = false;
            for (Source source : sourceOrder) {
                Meme m = buckets.get(source).poll();
                if (m != null) {
                    result.add(m);
                    anyPolled = true;
                }
            }
        }

        return result;
    }
}
