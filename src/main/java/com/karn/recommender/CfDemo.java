package com.karn.recommender;

import java.nio.file.Path;

/**
 * Runnable demo: loads the sample ratings, trains all three engines and
 * prints top-5 recommendations for one sci-fi fan (mia) and one comedy
 * fan (zoe). Usage: {@code CfDemo [path-to-csv] [user]}.
 */
public final class CfDemo {
    private CfDemo() {
    }

    public static void main(String[] args) throws Exception {
        Path csv = Path.of(args.length > 0 ? args[0] : "data/sample-ratings.csv");
        RatingsStore store = RatingsCsv.load(csv);
        System.out.printf("Loaded %d ratings from %d users over %d items.%n",
                store.size(), store.users().size(), store.items().size());

        MatrixFactorization mf = new MatrixFactorization(8);
        mf.train(store, 200, 0.02, 0.01);
        System.out.printf("Matrix factorisation train RMSE: %.3f%n", mf.rmse(store));

        Recommender userKnn = new UserKnnRecommender(store, 3);
        Recommender itemKnn = new ItemKnnRecommender(store, 3);
        for (String user : args.length > 1 ? new String[]{args[1]} : new String[]{"mia", "zoe"}) {
            System.out.println("\nTop recommendations for " + user + ":");
            System.out.println("  user-kNN: " + userKnn.recommend(user, 5));
            System.out.println("  item-kNN: " + itemKnn.recommend(user, 5));
            System.out.println("  MF:       " + mf.recommend(user, 5));
        }
    }
}
