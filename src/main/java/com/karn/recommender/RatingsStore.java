package com.karn.recommender;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * In-memory ratings store. Indexes ratings both by user and by item so
 * neighbourhood methods can look up either side in O(1).
 */
public final class RatingsStore {
    private final Map<String, Map<String, Double>> byUser = new LinkedHashMap<>();
    private final Map<String, Map<String, Double>> byItem = new LinkedHashMap<>();
    private final List<Rating> all = new ArrayList<>();

    public void add(Rating rating) {
        byUser.computeIfAbsent(rating.userId(), k -> new LinkedHashMap<>())
                .put(rating.itemId(), rating.value());
        byItem.computeIfAbsent(rating.itemId(), k -> new LinkedHashMap<>())
                .put(rating.userId(), rating.value());
        all.add(rating);
    }

    public void add(String userId, String itemId, double value) {
        add(new Rating(userId, itemId, value));
    }

    /** Ratings by one user: itemId -> value. Never null. */
    public Map<String, Double> ratingsForUser(String userId) {
        return byUser.getOrDefault(userId, Map.of());
    }

    /** Ratings for one item: userId -> value. Never null. */
    public Map<String, Double> ratingsForItem(String itemId) {
        return byItem.getOrDefault(itemId, Map.of());
    }

    public Set<String> users() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(byUser.keySet()));
    }

    public Set<String> items() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(byItem.keySet()));
    }

    public List<Rating> allRatings() {
        return List.copyOf(all);
    }

    public int size() {
        return all.size();
    }

    public boolean hasRated(String userId, String itemId) {
        return byUser.getOrDefault(userId, Map.of()).containsKey(itemId);
    }

    public double globalMean() {
        return all.stream().mapToDouble(Rating::value).average().orElse(0.0);
    }

    public double userMean(String userId) {
        return ratingsForUser(userId).values().stream()
                .mapToDouble(Double::doubleValue).average().orElse(globalMean());
    }

    public double itemMean(String itemId) {
        return ratingsForItem(itemId).values().stream()
                .mapToDouble(Double::doubleValue).average().orElse(globalMean());
    }
}
