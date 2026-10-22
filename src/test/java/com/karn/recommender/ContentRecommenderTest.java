package com.karn.recommender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ContentRecommenderTest {
    private RatingsStore store;
    private ContentRecommender content;

    @BeforeEach
    void setUp() throws Exception {
        store = RatingsCsv.load(Path.of("data/sample-ratings.csv"));
        content = new ContentRecommender(store, ItemCatalog.load(Path.of("data/sample-items.csv")).texts());
    }

    @Test
    void ranksUnseenSciFiFirstForSciFiFan() {
        List<ScoredItem> recs = content.recommend("mia", 2);
        assertEquals("arrival", recs.get(0).itemId());
        assertEquals("step-brothers", recs.get(1).itemId());
    }

    @Test
    void sciFiItemScoresHigherForSciFiFanThanComedyFan() {
        // mia's liked profile is sci-fi terms, zoe's is comedy terms; arrival
        // shares only sci-fi terms, so it must score higher for mia.
        assertTrue(content.predict("mia", "arrival") > content.predict("zoe", "arrival"));
    }

    @Test
    void predictReturnsKnownRatingsAndFallsBackForColdUsers() {
        assertEquals(5.0, content.predict("mia", "dune"), 1e-9);
        // Cold user: no profile, so the item mean is the honest answer.
        assertEquals(store.itemMean("arrival"), content.predict("nobody", "arrival"), 1e-9);
    }

    @Test
    void profileIsEmptyForColdUser() {
        assertTrue(content.userProfile("nobody").isEmpty());
    }
}
