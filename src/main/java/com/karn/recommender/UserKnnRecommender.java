package com.karn.recommender;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * User-based k-NN: predict a rating from the k most similar users who rated
 * the item, weighting each neighbour's mean-centred rating by Pearson
 * similarity. Falls back to the user's mean, then the global mean.
 */
public final class UserKnnRecommender implements Recommender {
    private final RatingsStore store;
    private final int k;
    private final double minSimilarity;

    public UserKnnRecommender(RatingsStore store, int k, double minSimilarity) {
        if (k < 1) {
            throw new IllegalArgumentException("k must be >= 1");
        }
        this.store = store;
        this.k = k;
        this.minSimilarity = minSimilarity;
    }

    public UserKnnRecommender(RatingsStore store, int k) {
        this(store, k, 0.0);
    }

    @Override
    public double predict(String userId, String itemId) {
        Map<String, Double> userRatings = store.ratingsForUser(userId);
        if (userRatings.containsKey(itemId)) {
            return userRatings.get(itemId);
        }
        double userMean = store.userMean(userId);
        List<Neighbour> neighbours = new ArrayList<>();
        for (String other : store.users()) {
            if (other.equals(userId)) {
                continue;
            }
            Map<String, Double> otherRatings = store.ratingsForUser(other);
            if (!otherRatings.containsKey(itemId)) {
                continue;
            }
            double sim = Similarity.pearson(userRatings, otherRatings);
            if (sim > minSimilarity) {
                neighbours.add(new Neighbour(sim, otherRatings.get(itemId) - store.userMean(other)));
            }
        }
        neighbours.sort(Comparator.comparingDouble(Neighbour::similarity).reversed());
        double num = 0.0;
        double den = 0.0;
        for (Neighbour n : neighbours.subList(0, Math.min(k, neighbours.size()))) {
            num += n.similarity() * n.centredRating();
            den += Math.abs(n.similarity());
        }
        if (den == 0.0) {
            return userMean;
        }
        return userMean + num / den;
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

    private record Neighbour(double similarity, double centredRating) {
    }
}
