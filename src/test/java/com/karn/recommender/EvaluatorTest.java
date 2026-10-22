package com.karn.recommender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class EvaluatorTest {
    /** A stub recommender returning a fixed ranking and fixed predictions. */
    private static Recommender stub(List<String> ranking, Map<String, Double> predictions) {
        return new Recommender() {
            @Override
            public double predict(String userId, String itemId) {
                return predictions.getOrDefault(itemId, 0.0);
            }

            @Override
            public List<ScoredItem> recommend(String userId, int n) {
                return ranking.stream().limit(n)
                        .map(id -> new ScoredItem(id, 5.0))
                        .toList();
            }
        };
    }

    private static Evaluator.Holdout singleUserHoldout(String heldOutItem, double rating) {
        RatingsStore train = new RatingsStore();
        train.add("ana", "dune", 5);
        return new Evaluator.Holdout(train,
                Map.of("ana", new Rating("ana", heldOutItem, rating)));
    }

    @Test
    void leaveOneOutHoldsOutHighestRatedItem() {
        RatingsStore store = new RatingsStore();
        store.add("ana", "dune", 5);
        store.add("ana", "matrix", 4);
        store.add("ana", "arrival", 3);
        store.add("ben", "dune", 5);
        store.add("ben", "matrix", 4);

        Evaluator.Holdout holdout = Evaluator.leaveOneOut(store);
        assertEquals("dune", holdout.test().get("ana").itemId());
        assertEquals(5.0, holdout.test().get("ana").value(), 1e-9);
        assertFalse(holdout.test().containsKey("ben"), "two-rating users stay fully in train");
        assertFalse(holdout.train().hasRated("ana", "dune"));
        assertTrue(holdout.train().hasRated("ana", "matrix"));
        assertTrue(holdout.train().hasRated("ben", "dune"));
    }

    @Test
    void metricsMatchHandComputedValuesForRankTwoHit() {
        // Relevant item "b" at rank 2 of 3: P@3 = 1/3, R@3 = 1,
        // NDCG@3 = 1/log2(3); prediction 4 vs actual 5 gives RMSE 1.
        Recommender rec = stub(List.of("a", "b", "c"), Map.of("b", 4.0));
        Evaluator.Result result = Evaluator.evaluate(rec, singleUserHoldout("b", 5.0), 3);
        assertEquals(1.0 / 3.0, result.precisionAtK(), 1e-9);
        assertEquals(1.0, result.recallAtK(), 1e-9);
        assertEquals(1.0 / (Math.log(3) / Math.log(2)), result.ndcgAtK(), 1e-9);
        assertEquals(1.0, result.rmse(), 1e-9);
        assertEquals(1, result.users());
    }

    @Test
    void missScoresZeroAcrossRankingMetrics() {
        Recommender rec = stub(List.of("a", "c"), Map.of("b", 3.0));
        Evaluator.Result result = Evaluator.evaluate(rec, singleUserHoldout("b", 5.0), 3);
        assertEquals(0.0, result.precisionAtK(), 1e-9);
        assertEquals(0.0, result.recallAtK(), 1e-9);
        assertEquals(0.0, result.ndcgAtK(), 1e-9);
        assertEquals(2.0, result.rmse(), 1e-9);
    }

    @Test
    void emptyHoldoutScoresZeroWithoutDividingByZero() {
        RatingsStore empty = new RatingsStore();
        Evaluator.Result result = Evaluator.evaluate(
                stub(List.of(), Map.of()), Evaluator.leaveOneOut(empty), 5);
        assertEquals(0, result.users());
        assertEquals(0.0, result.rmse(), 1e-9);
    }

    @Test
    void evaluateAllAndTableCoverEveryEngine() {
        Evaluator.Holdout holdout = singleUserHoldout("b", 5.0);
        Map<String, Evaluator.Result> results = Evaluator.evaluateAll(
                Map.of("hit", stub(List.of("b"), Map.of("b", 5.0)),
                        "miss", stub(List.of("a"), Map.of("b", 1.0))),
                holdout, 5);
        assertEquals(2, results.size());
        String table = Evaluator.table(results, 5);
        assertTrue(table.contains("hit"));
        assertTrue(table.contains("miss"));
        // Best NDCG first: the hitting engine's row precedes the missing one.
        assertTrue(table.indexOf("hit") < table.indexOf("miss"));
    }
}
