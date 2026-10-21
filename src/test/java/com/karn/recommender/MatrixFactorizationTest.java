package com.karn.recommender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MatrixFactorizationTest {
    /** Ratings generated from exact rank-2 factors: rating = 3 + dot(pu, qi). */
    private static RatingsStore lowRankStore() {
        double[][] users = {{1, 0}, {0, 1}, {1, 1}, {2, 0}};
        double[][] items = {{1, 0}, {0, 1}, {1, 1}, {-1, 1}, {0.5, 0.5}};
        RatingsStore store = new RatingsStore();
        for (int u = 0; u < users.length; u++) {
            for (int i = 0; i < items.length; i++) {
                double rating = 3 + users[u][0] * items[i][0] + users[u][1] * items[i][1];
                store.add("u" + u, "i" + i, rating);
            }
        }
        return store;
    }

    @Test
    void recoversLowRankStructure() {
        RatingsStore store = lowRankStore();
        MatrixFactorization mf = new MatrixFactorization(2);
        mf.train(store, 300, 0.05, 0.001);
        assertTrue(mf.rmse(store) < 0.35, "train RMSE was " + mf.rmse(store));
    }

    @Test
    void sameSeedGivesSamePredictions() {
        RatingsStore store = lowRankStore();
        MatrixFactorization a = new MatrixFactorization(2, 7L);
        MatrixFactorization b = new MatrixFactorization(2, 7L);
        a.train(store, 50, 0.05, 0.01);
        b.train(store, 50, 0.05, 0.01);
        assertEquals(a.predict("u0", "i0"), b.predict("u0", "i0"), 1e-9);
    }

    @Test
    void untrainedModelRecommendsNothing() {
        MatrixFactorization mf = new MatrixFactorization(2);
        assertTrue(mf.recommend("u0", 5).isEmpty());
    }
}
