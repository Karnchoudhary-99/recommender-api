package com.karn.recommender;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Hybrid combiner: a weighted average of component recommenders'
 * predictions. Weights are normalized, so {@code (cf, 0.7)} +
 * {@code (content, 0.3)} really means 70/30. Every component predicts
 * on the same 1-5 scale (content scores are mapped there), so the
 * blend stays interpretable. Cold users/items degrade to whatever
 * each component falls back to -- usually a mean -- rather than failing.
 */
public final class HybridRecommender implements Recommender {
    private final RatingsStore store;
    private final List<Weighted> components;

    public record Weighted(Recommender recommender, double weight) {
    }

    public HybridRecommender(RatingsStore store, List<Weighted> components) {
        if (components == null || components.isEmpty()) {
            throw new IllegalArgumentException("hybrid needs at least one component");
        }
        double total = 0.0;
        for (Weighted w : components) {
            if (w.weight() < 0) {
                throw new IllegalArgumentException("weights must be >= 0");
            }
            total += w.weight();
        }
        if (total == 0.0) {
            throw new IllegalArgumentException("weights must sum to > 0");
        }
        List<Weighted> normalized = new ArrayList<>();
        for (Weighted w : components) {
            normalized.add(new Weighted(w.recommender(), w.weight() / total));
        }
        this.store = store;
        this.components = List.copyOf(normalized);
    }

    public HybridRecommender(RatingsStore store, Recommender first, double firstWeight,
            Recommender second, double secondWeight) {
        this(store, List.of(new Weighted(first, firstWeight), new Weighted(second, secondWeight)));
    }

    @Override
    public double predict(String userId, String itemId) {
        double score = 0.0;
        for (Weighted w : components) {
            score += w.weight() * w.recommender().predict(userId, itemId);
        }
        return score;
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
}
