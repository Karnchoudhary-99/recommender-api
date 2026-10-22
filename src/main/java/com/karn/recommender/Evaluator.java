package com.karn.recommender;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Offline evaluation for recommenders: a deterministic leave-one-out
 * holdout split plus precision@k, recall@k, NDCG@k and prediction RMSE.
 *
 * <p>Holdout rule: for every user with at least three ratings, the
 * highest-rated item is held out (ties break by first rating in file
 * order, since {@link RatingsStore} preserves insertion order). The
 * held-out item is therefore relevant by construction, and it is
 * unseen in the training store, so {@link Recommender#recommend} can
 * legitimately surface it. Users with fewer than three ratings are
 * skipped: holding out from a one- or two-rating user would leave a
 * training profile too thin to learn from.
 *
 * <p>Metrics are the standard definitions, averaged over evaluated
 * users: precision@k = hits / k, recall@k = hits / relevant, NDCG@k
 * with binary relevance (a held-out item in the top-k counts 1,
 * discounted by log2(rank + 1)), and RMSE over the held-out
 * predictions. A recommender that returns fewer than k items is
 * still scored over k, so short lists are not rewarded.
 */
public final class Evaluator {
    private Evaluator() {
    }

    /** A training store plus the held-out (user -&gt; rating) test set. */
    public record Holdout(RatingsStore train, Map<String, Rating> test) {
    }

    /** Averaged metrics for one engine at one k, plus how many users were scored. */
    public record Result(double precisionAtK, double recallAtK, double ndcgAtK,
                         double rmse, int users) {
        @Override
        public String toString() {
            return String.format("P@k=%.3f R@k=%.3f NDCG@k=%.3f RMSE=%.3f (n=%d)",
                    precisionAtK, recallAtK, ndcgAtK, rmse, users);
        }
    }

    /** Leave-one-out split: highest-rated item per eligible user held out. */
    public static Holdout leaveOneOut(RatingsStore store) {
        RatingsStore train = new RatingsStore();
        Map<String, Rating> test = new LinkedHashMap<>();
        for (String user : store.users()) {
            Map<String, Double> ratings = store.ratingsForUser(user);
            if (ratings.size() < 3) {
                for (Map.Entry<String, Double> e : ratings.entrySet()) {
                    train.add(user, e.getKey(), e.getValue());
                }
                continue;
            }
            String heldOut = null;
            double best = Double.NEGATIVE_INFINITY;
            for (Map.Entry<String, Double> e : ratings.entrySet()) {
                if (e.getValue() > best) {
                    best = e.getValue();
                    heldOut = e.getKey();
                }
            }
            for (Map.Entry<String, Double> e : ratings.entrySet()) {
                if (e.getKey().equals(heldOut)) {
                    test.put(user, new Rating(user, heldOut, e.getValue()));
                } else {
                    train.add(user, e.getKey(), e.getValue());
                }
            }
        }
        return new Holdout(train, test);
    }

    /** Score one recommender against a holdout at cutoff k. */
    public static Result evaluate(Recommender recommender, Holdout holdout, int k) {
        if (k < 1) {
            throw new IllegalArgumentException("k must be >= 1");
        }
        double precisionSum = 0.0;
        double recallSum = 0.0;
        double ndcgSum = 0.0;
        double squaredErrorSum = 0.0;
        int users = 0;
        for (Map.Entry<String, Rating> entry : holdout.test().entrySet()) {
            String user = entry.getKey();
            String heldOutItem = entry.getValue().itemId();
            List<ScoredItem> top = recommender.recommend(user, k);
            int hits = 0;
            double dcg = 0.0;
            for (int rank = 0; rank < top.size(); rank++) {
                if (top.get(rank).itemId().equals(heldOutItem)) {
                    hits++;
                    dcg += 1.0 / (Math.log(rank + 2) / Math.log(2));
                }
            }
            // One relevant item per user, so the ideal DCG is 1.0.
            precisionSum += (double) hits / k;
            recallSum += hits; // hits / 1 relevant item
            ndcgSum += dcg;
            double error = recommender.predict(user, heldOutItem) - entry.getValue().value();
            squaredErrorSum += error * error;
            users++;
        }
        if (users == 0) {
            return new Result(0.0, 0.0, 0.0, 0.0, 0);
        }
        return new Result(precisionSum / users, recallSum / users, ndcgSum / users,
                Math.sqrt(squaredErrorSum / users), users);
    }

    /** Evaluate several named engines and return one result per name, in order. */
    public static Map<String, Result> evaluateAll(Map<String, Recommender> engines,
                                                  Holdout holdout, int k) {
        Map<String, Result> results = new LinkedHashMap<>();
        for (Map.Entry<String, Recommender> e : engines.entrySet()) {
            results.put(e.getKey(), evaluate(e.getValue(), holdout, k));
        }
        return results;
    }

    /** Render a results table, best NDCG first, for demos and logs. */
    public static String table(Map<String, Result> results, int k) {
        List<Map.Entry<String, Result>> rows = new ArrayList<>(results.entrySet());
        rows.sort((a, b) -> Double.compare(b.getValue().ndcgAtK(), a.getValue().ndcgAtK()));
        StringBuilder out = new StringBuilder();
        out.append(String.format("%-10s %8s %8s %8s %8s%n", "engine",
                "P@" + k, "R@" + k, "NDCG@" + k, "RMSE"));
        for (Map.Entry<String, Result> row : rows) {
            Result r = row.getValue();
            out.append(String.format("%-10s %8.3f %8.3f %8.3f %8.3f%n",
                    row.getKey(), r.precisionAtK(), r.recallAtK(), r.ndcgAtK(), r.rmse()));
        }
        return out.toString();
    }
}
