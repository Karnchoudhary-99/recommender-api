package com.karn.recommender;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Vector similarity over sparse rating maps (key -> value). */
public final class Similarity {
    private Similarity() {
    }

    /**
     * Cosine similarity over the union of keys. Vectors that share no key
     * score 0; a zero-norm vector scores 0 against everything.
     */
    public static double cosine(Map<String, Double> a, Map<String, Double> b) {
        double dot = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (double v : a.values()) {
            normA += v * v;
        }
        for (double v : b.values()) {
            normB += v * v;
        }
        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }
        for (Map.Entry<String, Double> e : a.entrySet()) {
            Double other = b.get(e.getKey());
            if (other != null) {
                dot += e.getValue() * other;
            }
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /**
     * Pearson correlation over the keys both vectors share, each centred on
     * its mean over the shared keys. Needs at least 2 shared keys and
     * non-zero variance on both sides, else 0.
     */
    public static double pearson(Map<String, Double> a, Map<String, Double> b) {
        Set<String> common = new HashSet<>(a.keySet());
        common.retainAll(b.keySet());
        if (common.size() < 2) {
            return 0.0;
        }
        double meanA = common.stream().mapToDouble(k -> a.get(k)).average().orElse(0.0);
        double meanB = common.stream().mapToDouble(k -> b.get(k)).average().orElse(0.0);
        double num = 0.0;
        double denA = 0.0;
        double denB = 0.0;
        for (String k : common) {
            double da = a.get(k) - meanA;
            double db = b.get(k) - meanB;
            num += da * db;
            denA += da * da;
            denB += db * db;
        }
        if (denA == 0.0 || denB == 0.0) {
            return 0.0;
        }
        return num / (Math.sqrt(denA) * Math.sqrt(denB));
    }
}
