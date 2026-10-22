package com.karn.recommender;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Content-based recommender: each item is a TF-IDF vector over its title
 * and description, and a user's profile is the rating-weighted average of
 * the vectors of items they liked. An unseen item scores by cosine
 * similarity to that profile, mapped onto the 1-5 rating scale.
 *
 * <p>Items with no catalog text get no vector and fall back to the item
 * mean, so a partial catalog never breaks recommendations.
 */
public final class ContentRecommender implements Recommender {
    private final RatingsStore store;
    private final Map<String, Map<String, Double>> itemVectors;

    public ContentRecommender(RatingsStore store, Map<String, String> itemTexts) {
        this.store = store;
        TfidfVectorizer vectorizer = new TfidfVectorizer();
        vectorizer.fit(itemTexts.values());
        Map<String, Map<String, Double>> vectors = new HashMap<>();
        for (Map.Entry<String, String> e : itemTexts.entrySet()) {
            vectors.put(e.getKey(), vectorizer.vectorize(e.getValue()));
        }
        this.itemVectors = Map.copyOf(vectors);
    }

    /** Rating-weighted profile vector for a user; empty for cold users. */
    public Map<String, Double> userProfile(String userId) {
        Map<String, Double> rated = store.ratingsForUser(userId);
        double userMean = store.userMean(userId);
        Map<String, Double> weighted = new HashMap<>();
        double totalWeight = 0.0;
        for (Map.Entry<String, Double> e : rated.entrySet()) {
            Map<String, Double> vec = itemVectors.get(e.getKey());
            if (vec == null || vec.isEmpty()) {
                continue;
            }
            double weight = e.getValue() - userMean;
            if (weight > 0) {
                for (Map.Entry<String, Double> term : vec.entrySet()) {
                    weighted.merge(term.getKey(), weight * term.getValue(), Double::sum);
                }
                totalWeight += weight;
            }
        }
        if (totalWeight == 0.0) {
            // Flat ratings (or no liked items): fall back to plain average.
            for (Map.Entry<String, Double> e : rated.entrySet()) {
                Map<String, Double> vec = itemVectors.get(e.getKey());
                if (vec == null || vec.isEmpty()) {
                    continue;
                }
                for (Map.Entry<String, Double> term : vec.entrySet()) {
                    weighted.merge(term.getKey(), term.getValue(), Double::sum);
                }
                totalWeight += 1.0;
            }
        }
        if (totalWeight == 0.0) {
            return Map.of();
        }
        double norm = 0.0;
        for (Map.Entry<String, Double> e : weighted.entrySet()) {
            e.setValue(e.getValue() / totalWeight);
            norm += e.getValue() * e.getValue();
        }
        norm = Math.sqrt(norm);
        if (norm == 0.0) {
            return Map.of();
        }
        for (Map.Entry<String, Double> e : weighted.entrySet()) {
            e.setValue(e.getValue() / norm);
        }
        return weighted;
    }

    @Override
    public double predict(String userId, String itemId) {
        Map<String, Double> rated = store.ratingsForUser(userId);
        if (rated.containsKey(itemId)) {
            return rated.get(itemId);
        }
        Map<String, Double> profile = userProfile(userId);
        Map<String, Double> vec = itemVectors.get(itemId);
        if (profile.isEmpty() || vec == null || vec.isEmpty()) {
            return store.itemMean(itemId);
        }
        double cosine = TfidfVectorizer.cosine(profile, vec);
        return Math.min(5.0, Math.max(1.0, 3.0 + 2.0 * cosine));
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
