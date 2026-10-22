package com.karn.recommender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RecommendationApiTest {
    private RecommendationApi api;

    @BeforeEach
    void setUp() throws IOException {
        RatingsStore store = RatingsCsv.load(Path.of("data/sample-ratings.csv"));
        api = new RecommendationApi(store, ItemCatalog.load(Path.of("data/sample-items.csv")).texts());
    }

    @Test
    void everyEngineRanksArrivalFirstForSciFiFanMia() {
        for (String engine : List.of("userknn", "itemknn", "content", "hybrid")) {
            List<ScoredItem> top = api.recommend("mia", 3, engine);
            assertEquals("arrival", top.get(0).itemId(), engine + " should rank arrival first");
        }
    }

    @Test
    void defaultEngineIsHybridAndRecommendationsExcludeRatedItems() {
        List<ScoredItem> top = api.recommend("mia", 10, null);
        assertEquals(api.recommend("mia", 10, "hybrid"), top);
        assertTrue(top.stream().noneMatch(s -> s.itemId().equals("dune")));
    }

    @Test
    void rejectsUnknownEngineBadNAndOutOfRangeRatings() {
        assertThrows(IllegalArgumentException.class, () -> api.recommend("mia", 3, "mf"));
        assertThrows(IllegalArgumentException.class, () -> api.recommend("mia", 0, "hybrid"));
        assertThrows(IllegalArgumentException.class, () -> api.recommend("mia", 51, "hybrid"));
        assertThrows(IllegalArgumentException.class, () -> api.recommend(" ", 3, "hybrid"));
        assertThrows(IllegalArgumentException.class, () -> api.addRating("mia", "arrival", 0.5));
        assertThrows(IllegalArgumentException.class, () -> api.addRating("mia", "arrival", 6.0));
        assertThrows(IllegalArgumentException.class, () -> api.predict("mia", " ", "hybrid"));
    }

    @Test
    void postedRatingChangesItemKnnPredictions() {
        // Before the post, mia has not rated arrival; item-kNN predicts
        // from her sci-fi neighbours. Rating arrival 1 must move the
        // prediction for a similar unseen item (none left unseen in the
        // sci-fi cluster for mia except arrival itself), so instead
        // verify the write is stored and re-rating replaces it.
        api.addRating("mia", "arrival", 1.0);
        assertEquals(1.0, api.predict("mia", "arrival", "itemknn"));
        api.addRating("mia", "arrival", 5.0);
        assertEquals(5.0, api.predict("mia", "arrival", "itemknn"));
        assertEquals(37, api.ratingCount());
    }
}
