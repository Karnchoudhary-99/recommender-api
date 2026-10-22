package com.karn.recommender;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The engine registry behind the HTTP API: one named recommender per
 * approach, plus rating ingestion. Engine names are the API's
 * {@code engine} parameter values: {@code userknn}, {@code itemknn},
 * {@code content}, {@code hybrid} (the default -- 70% user-kNN,
 * 30% content, the same blend the demo uses).
 *
 * <p>All engines read the same {@link RatingsStore} live, except
 * item-kNN, whose similarity cache is rebuilt after every rating write
 * so a POSTed rating is visible to the next request instead of serving
 * stale similarities.
 */
public final class RecommendationApi {
    public static final Set<String> ENGINES = Set.of("userknn", "itemknn", "content", "hybrid");
    public static final String DEFAULT_ENGINE = "hybrid";
    private static final int K = 3;
    private static final int MAX_N = 50;

    private final RatingsStore store;
    private final Map<String, Recommender> engines = new LinkedHashMap<>();

    public RecommendationApi(RatingsStore store, Map<String, String> itemTexts) {
        if (store == null) {
            throw new IllegalArgumentException("store must not be null");
        }
        this.store = store;
        Recommender userKnn = new UserKnnRecommender(store, K);
        Recommender content = new ContentRecommender(store, itemTexts == null ? Map.of() : itemTexts);
        engines.put("userknn", userKnn);
        engines.put("itemknn", new ItemKnnRecommender(store, K));
        engines.put("content", content);
        engines.put("hybrid", new HybridRecommender(store, userKnn, 0.7, content, 0.3));
    }

    /** Engine names in stable order, for the API's engine listing. */
    public Set<String> engineNames() {
        return Set.copyOf(engines.keySet());
    }

    public List<ScoredItem> recommend(String userId, int n, String engineName) {
        requireUser(userId);
        if (n < 1 || n > MAX_N) {
            throw new IllegalArgumentException("n must be between 1 and " + MAX_N);
        }
        return engine(engineName).recommend(userId, n);
    }

    public double predict(String userId, String itemId, String engineName) {
        requireUser(userId);
        if (itemId == null || itemId.isBlank()) {
            throw new IllegalArgumentException("itemId must not be blank");
        }
        return engine(engineName).predict(userId, itemId);
    }

    /** Add (or replace) a rating; returns the stored value. */
    public double addRating(String userId, String itemId, double value) {
        requireUser(userId);
        if (itemId == null || itemId.isBlank()) {
            throw new IllegalArgumentException("itemId must not be blank");
        }
        if (value < 1.0 || value > 5.0) {
            throw new IllegalArgumentException("rating must be between 1 and 5");
        }
        store.add(userId, itemId, value);
        // Item-kNN caches item similarities; rebuild it so the new
        // rating is not shadowed by the old cache. The hybrid blends
        // user-kNN + content, both of which read the store live.
        engines.put("itemknn", new ItemKnnRecommender(store, K));
        return value;
    }

    public int ratingCount() {
        return store.size();
    }

    private Recommender engine(String engineName) {
        String name = engineName == null || engineName.isBlank() ? DEFAULT_ENGINE : engineName;
        Recommender engine = engines.get(name);
        if (engine == null) {
            throw new IllegalArgumentException("unknown engine: " + name + " (known: userknn, itemknn, content, hybrid)");
        }
        return engine;
    }

    private static void requireUser(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId must not be blank");
        }
    }
}
