package com.karn.recommender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HybridRecommenderTest {
    private RatingsStore store;
    private UserKnnRecommender userKnn;
    private ContentRecommender content;

    @BeforeEach
    void setUp() throws Exception {
        store = RatingsCsv.load(Path.of("data/sample-ratings.csv"));
        userKnn = new UserKnnRecommender(store, 3);
        content = new ContentRecommender(store,
                ItemCatalog.load(Path.of("data/sample-items.csv")).texts());
    }

    @Test
    void weightsAreNormalized() {
        HybridRecommender onlyUserKnn = new HybridRecommender(store, userKnn, 5.0, content, 0.0);
        assertEquals(userKnn.predict("mia", "arrival"),
                onlyUserKnn.predict("mia", "arrival"), 1e-9);
    }

    @Test
    void blendIsWeightedAverageOfComponents() {
        HybridRecommender hybrid = new HybridRecommender(store, userKnn, 0.7, content, 0.3);
        double expected = 0.7 * userKnn.predict("mia", "arrival")
                + 0.3 * content.predict("mia", "arrival");
        assertEquals(expected, hybrid.predict("mia", "arrival"), 1e-9);
    }

    @Test
    void invalidConfigurationsThrow() {
        assertThrows(IllegalArgumentException.class,
                () -> new HybridRecommender(store, List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new HybridRecommender(store, userKnn, -1.0, content, 1.0));
        assertThrows(IllegalArgumentException.class,
                () -> new HybridRecommender(store, userKnn, 0.0, content, 0.0));
    }

    @Test
    void hybridRecommendsOnlyUnratedItems() {
        HybridRecommender hybrid = new HybridRecommender(store, userKnn, 0.5, content, 0.5);
        List<ScoredItem> recs = hybrid.recommend("mia", 5);
        assertEquals(List.of("arrival", "step-brothers"),
                recs.stream().map(ScoredItem::itemId).toList());
    }
}
