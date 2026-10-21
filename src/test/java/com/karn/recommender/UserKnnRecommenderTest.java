package com.karn.recommender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class UserKnnRecommenderTest {
    /** ana and ben share tastes; cid is their opposite. */
    private static RatingsStore store() {
        RatingsStore store = new RatingsStore();
        store.add("ana", "dune", 5);
        store.add("ana", "matrix", 4);
        store.add("ben", "dune", 5);
        store.add("ben", "matrix", 4);
        store.add("ben", "arrival", 5);
        store.add("cid", "dune", 1);
        store.add("cid", "matrix", 2);
        store.add("cid", "arrival", 1);
        return store;
    }

    @Test
    void predictsFromIdenticalTasteNeighbour() {
        UserKnnRecommender rec = new UserKnnRecommender(store(), 2);
        assertEquals(5.0, rec.predict("ana", "arrival"), 0.6);
    }

    @Test
    void unknownUserFallsBackToGlobalMean() {
        RatingsStore store = store();
        UserKnnRecommender rec = new UserKnnRecommender(store, 2);
        assertEquals(store.globalMean(), rec.predict("ghost", "arrival"), 1e-9);
    }

    @Test
    void recommendExcludesRatedItemsAndRanksBestFirst() {
        UserKnnRecommender rec = new UserKnnRecommender(store(), 2);
        List<ScoredItem> top = rec.recommend("ana", 5);
        assertEquals(1, top.size());
        assertEquals("arrival", top.get(0).itemId());
        assertTrue(top.get(0).score() > 0);
    }
}
