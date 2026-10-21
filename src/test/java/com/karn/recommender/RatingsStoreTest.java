package com.karn.recommender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RatingsStoreTest {
    @Test
    void indexesBothWays() {
        RatingsStore store = new RatingsStore();
        store.add("ana", "dune", 5);
        store.add("ben", "dune", 3);
        assertEquals(5.0, store.ratingsForUser("ana").get("dune"));
        assertEquals(2, store.ratingsForItem("dune").size());
        assertTrue(store.hasRated("ana", "dune"));
        assertFalse(store.hasRated("ana", "matrix"));
    }

    @Test
    void meansFallBackToGlobal() {
        RatingsStore store = new RatingsStore();
        store.add("ana", "dune", 4);
        store.add("ana", "matrix", 2);
        assertEquals(3.0, store.globalMean());
        assertEquals(3.0, store.userMean("ana"));
        assertEquals(3.0, store.userMean("ghost"));
        assertEquals(4.0, store.itemMean("dune"));
    }

    @Test
    void reRatingReplacesInsteadOfDuplicating() {
        RatingsStore store = new RatingsStore();
        store.add("ana", "dune", 5);
        store.add("ana", "dune", 1);
        assertEquals(1, store.size());
        assertEquals(1.0, store.ratingsForUser("ana").get("dune"));
    }

    @Test
    void blankIdsAndNaNRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Rating("", "dune", 5));
        assertThrows(IllegalArgumentException.class, () -> new Rating("ana", "dune", Double.NaN));
    }
}
