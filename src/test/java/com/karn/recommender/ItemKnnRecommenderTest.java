package com.karn.recommender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class ItemKnnRecommenderTest {
    private static RatingsStore store() {
        RatingsStore store = new RatingsStore();
        // dune and matrix are rated identically by everyone: perfect twins.
        store.add("ana", "dune", 5);
        store.add("ana", "matrix", 5);
        store.add("ben", "dune", 4);
        store.add("ben", "matrix", 4);
        store.add("cid", "dune", 2);
        store.add("cid", "matrix", 2);
        store.add("ana", "arrival", 1);
        return store;
    }

    @Test
    void twinItemsHaveSimilarityOneAndSymmetryHolds() {
        ItemKnnRecommender rec = new ItemKnnRecommender(store(), 2);
        assertEquals(1.0, rec.itemSimilarity("dune", "matrix"), 1e-9);
        assertEquals(rec.itemSimilarity("matrix", "dune"), rec.itemSimilarity("dune", "matrix"), 1e-9);
    }

    @Test
    void predictsFromTwinItemRating() {
        ItemKnnRecommender rec = new ItemKnnRecommender(store(), 2);
        // ben already rated matrix 4: predict returns the known rating.
        assertEquals(4.0, rec.predict("ben", "matrix"), 1e-9);
        // ben never rated arrival; arrival is similar to dune/matrix, both
        // rated 4 by ben, so the similarity-weighted prediction is 4.0.
        assertEquals(4.0, rec.predict("ben", "arrival"), 1e-9);
    }

    @Test
    void recommendExcludesRatedItems() {
        ItemKnnRecommender rec = new ItemKnnRecommender(store(), 2);
        List<ScoredItem> top = rec.recommend("ben", 5);
        assertEquals(1, top.size());
        assertEquals("arrival", top.get(0).itemId());
        assertTrue(top.get(0).score() >= 0);
    }
}
