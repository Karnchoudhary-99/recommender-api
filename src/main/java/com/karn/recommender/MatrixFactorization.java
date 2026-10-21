package com.karn.recommender;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Matrix factorisation with biases, trained by SGD:
 * pred(u,i) = globalMean + userBias[u] + itemBias[i] + dot(P[u], Q[i]).
 * Deterministic for a fixed seed, so results are reproducible.
 */
public final class MatrixFactorization implements Recommender {
    private final int factors;
    private final long seed;
    private final Map<String, double[]> userFactors = new HashMap<>();
    private final Map<String, double[]> itemFactors = new HashMap<>();
    private final Map<String, Double> userBias = new HashMap<>();
    private final Map<String, Double> itemBias = new HashMap<>();
    private double globalMean;
    private boolean trained;
    private RatingsStore trainedStore;

    public MatrixFactorization(int factors, long seed) {
        if (factors < 1) {
            throw new IllegalArgumentException("factors must be >= 1");
        }
        this.factors = factors;
        this.seed = seed;
    }

    public MatrixFactorization(int factors) {
        this(factors, 42L);
    }

    /** Trains for {@code epochs} passes over a shuffled copy of the ratings. */
    public void train(RatingsStore store, int epochs, double learningRate, double regularization) {
        globalMean = store.globalMean();
        trainedStore = store;
        Random random = new Random(seed);
        for (String user : store.users()) {
            userFactors.computeIfAbsent(user, u -> randomVector(random));
            userBias.putIfAbsent(user, 0.0);
        }
        for (String item : store.items()) {
            itemFactors.computeIfAbsent(item, i -> randomVector(random));
            itemBias.putIfAbsent(item, 0.0);
        }
        List<Rating> ratings = new ArrayList<>(store.allRatings());
        for (int epoch = 0; epoch < epochs; epoch++) {
            Collections.shuffle(ratings, random);
            for (Rating r : ratings) {
                double[] pu = userFactors.get(r.userId());
                double[] qi = itemFactors.get(r.itemId());
                double err = r.value() - predictRaw(r.userId(), r.itemId(), pu, qi);
                double bu = userBias.get(r.userId());
                double bi = itemBias.get(r.itemId());
                userBias.put(r.userId(), bu + learningRate * (err - regularization * bu));
                itemBias.put(r.itemId(), bi + learningRate * (err - regularization * bi));
                for (int f = 0; f < factors; f++) {
                    double puf = pu[f];
                    double qif = qi[f];
                    pu[f] = puf + learningRate * (err * qif - regularization * puf);
                    qi[f] = qif + learningRate * (err * puf - regularization * qif);
                }
            }
        }
        trained = true;
    }

    private double[] randomVector(Random random) {
        double[] v = new double[factors];
        for (int i = 0; i < factors; i++) {
            v[i] = random.nextGaussian() * 0.1;
        }
        return v;
    }

    private double predictRaw(String userId, String itemId, double[] pu, double[] qi) {
        double dot = 0.0;
        if (pu != null && qi != null) {
            for (int f = 0; f < factors; f++) {
                dot += pu[f] * qi[f];
            }
        }
        return globalMean + userBias.getOrDefault(userId, 0.0)
                + itemBias.getOrDefault(itemId, 0.0) + dot;
    }

    @Override
    public double predict(String userId, String itemId) {
        if (!trained) {
            return globalMean;
        }
        return predictRaw(userId, itemId,
                userFactors.get(userId), itemFactors.get(itemId));
    }

    /** Root-mean-square error of current predictions on the store's ratings. */
    public double rmse(RatingsStore store) {
        double sumSq = 0.0;
        for (Rating r : store.allRatings()) {
            double err = r.value() - predict(r.userId(), r.itemId());
            sumSq += err * err;
        }
        return store.size() == 0 ? 0.0 : Math.sqrt(sumSq / store.size());
    }

    @Override
    public List<ScoredItem> recommend(String userId, int n) {
        if (trainedStore == null) {
            return List.of();
        }
        return recommend(trainedStore, userId, n);
    }

    /** Top-n unrated items for the user, best first. */
    public List<ScoredItem> recommend(RatingsStore store, String userId, int n) {
        Map<String, Double> rated = store.ratingsForUser(userId);
        List<ScoredItem> scored = new ArrayList<>();
        for (String item : store.items()) {
            if (!rated.containsKey(item)) {
                scored.add(new ScoredItem(item, predict(userId, item)));
            }
        }
        scored.sort(null);
        return scored.subList(0, Math.min(n, scored.size()));
    }
}
