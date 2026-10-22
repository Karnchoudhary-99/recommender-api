package com.karn.recommender;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Runnable evaluation: loads a ratings CSV (and an item catalog for the
 * content engine), holds out each user's top-rated item, trains every
 * engine on the remainder, and prints a precision/recall/NDCG/RMSE
 * table at k. Usage: {@code EvalDemo [ratingsCsv] [itemsCsv] [k]}.
 *
 * <p>Defaults match the bundled sample data, where the holdout is
 * small (8 users) and every engine's unseen-item list is short, so
 * treat the absolute numbers as a smoke signal, not a benchmark:
 * the point is that all engines are scored by the same honest split.
 */
public final class EvalDemo {
    private EvalDemo() {
    }

    public static void main(String[] args) throws Exception {
        Path ratingsCsv = Path.of(args.length > 0 ? args[0] : "data/sample-ratings.csv");
        Path itemsCsv = Path.of(args.length > 1 ? args[1] : "data/sample-items.csv");
        int k = args.length > 2 ? Integer.parseInt(args[2]) : 5;

        RatingsStore full = RatingsCsv.load(ratingsCsv);
        Evaluator.Holdout holdout = Evaluator.leaveOneOut(full);
        System.out.printf("Loaded %d ratings; held out %d items (one per user with >= 3 ratings).%n",
                full.size(), holdout.test().size());
        for (Map.Entry<String, Rating> e : holdout.test().entrySet()) {
            System.out.printf("  held out: %s -> %s (%.0f)%n",
                    e.getKey(), e.getValue().itemId(), e.getValue().value());
        }

        RatingsStore train = holdout.train();
        MatrixFactorization mf = new MatrixFactorization(8);
        mf.train(train, 200, 0.02, 0.01);
        Recommender userKnn = new UserKnnRecommender(train, 3);
        Recommender content = new ContentRecommender(train,
                ItemCatalog.load(itemsCsv).texts());
        Map<String, Recommender> engines = new LinkedHashMap<>();
        engines.put("userknn", userKnn);
        engines.put("itemknn", new ItemKnnRecommender(train, 3));
        engines.put("mf", mf);
        engines.put("content", content);
        engines.put("hybrid", new HybridRecommender(train, userKnn, 0.7, content, 0.3));

        Map<String, Evaluator.Result> results = Evaluator.evaluateAll(engines, holdout, k);
        System.out.println();
        System.out.print(Evaluator.table(results, k));
    }
}
