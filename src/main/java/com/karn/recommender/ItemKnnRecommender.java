package com.karn.recommender;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Item-based k-NN: predict a rating from the user's own ratings on the k
 * most similar items, weighting by cosine similarity between item rating
 * vectors. Item-item similarities are computed lazily and cached.
 */
public final class ItemKnnRecommender implements Recommender {
    private final RatingsStore store;
    private final int k;
    private final Map<String, Map<String, Double>> similarityCache = new HashMap<>();

    public ItemKnnRecommender(RatingsStore store, int k) {
        if (k < 1) {
            throw new IllegalArgumentException("k must be >= 1");
        }
        this.store = store;
        this.k = k;
    }

    /** Cosine similarity between two items over their shared raters. */
    public double itemSimilarity(String itemA, String itemB) {
        if (itemA.equals(itemB)) {
            return 1.0;
        }
        Map<String, Double> cached = similarityCache.computeIfAbsent(itemA, key -> new HashMap<>());
        Double hit = cached.get(itemB);
        if (hit != null) {
            return hit;
        }
        double sim = Similarity.cosine(store.ratingsForItem(itemA), store.ratingsForItem(itemB));
        cached.put(itemB, sim);
        similarityCache.computeIfAbsent(itemB, key -> new HashMap<>()).put(itemA, sim);
        return sim;
    }

    @Override
    public double predict(String userId, String itemId) {
        Map<String, Double> userRatings = store.ratingsForUser(userId);
        if (userRatings.containsKey(itemId)) {
            return userRatings.get(itemId);
        }
        List<Neighbour> neighbours = new ArrayList<>();
        for (Map.Entry<String, Double> e : userRatings.entrySet()) {
            double sim = itemSimilarity(itemId, e.getKey());
            if (sim > 0.0) {
                neighbours.add(new Neighbour(sim, e.getValue()));
            }
        }
        neighbours.sort((a, b) -> Double.compare(b.similarity(), a.similarity()));
        double num = 0.0;
        double den = 0.0;
        for (Neighbour n : neighbours.subList(0, Math.min(k, neighbours.size()))) {
            num += n.similarity() * n.rating();
            den += Math.abs(n.similarity());
        }
        if (den == 0.0) {
            return store.userMean(userId);
        }
        return num / den;
    }

    @Override
    public List<ScoredItem> recommend(String userId, int n) {
        Map<String, Double> rated = store.ratingsForUser(userId);
        List<ScoredItem> scored = new ArrayList<>();
        for (String item : store.items()) {
            if (!rated.containsKey(item)) {
                scored.add(new ScoredItem(item, predict(userId, item)));
            }
        }
        scored.sort(null);
        return scored.subList(0, Math.min(n, scored.size()));
    }

    private record Neighbour(double similarity, double rating) {
    }
}
