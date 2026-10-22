package com.karn.recommender;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * TF-IDF over tokenized text with smoothed IDF
 * ({@code log((N + 1) / (df + 1)) + 1}) and L2-normalized document
 * vectors, so cosine similarity is a plain dot product. Unknown terms
 * at vectorize time are ignored (they carry no fitted IDF).
 */
public final class TfidfVectorizer {
    private final Map<String, Double> idf = new HashMap<>();

    /** Fits IDF from a corpus of raw documents. Must be called before vectorize. */
    public void fit(Collection<String> documents) {
        Map<String, Integer> docFreq = new HashMap<>();
        int n = 0;
        for (String doc : documents) {
            n++;
            Set<String> seen = new HashSet<>(Tokenizer.tokenize(doc));
            for (String term : seen) {
                docFreq.merge(term, 1, Integer::sum);
            }
        }
        idf.clear();
        for (Map.Entry<String, Integer> e : docFreq.entrySet()) {
            idf.put(e.getKey(), Math.log((n + 1.0) / (e.getValue() + 1.0)) + 1.0);
        }
    }

    public boolean isFitted() {
        return !idf.isEmpty();
    }

    /** L2-normalized TF-IDF vector for raw text; empty if nothing is known. */
    public Map<String, Double> vectorize(String text) {
        if (idf.isEmpty()) {
            throw new IllegalStateException("TfidfVectorizer must be fitted before vectorize");
        }
        Map<String, Integer> counts = new HashMap<>();
        for (String token : Tokenizer.tokenize(text)) {
            if (idf.containsKey(token)) {
                counts.merge(token, 1, Integer::sum);
            }
        }
        Map<String, Double> vec = new HashMap<>();
        double norm = 0.0;
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            double v = e.getValue() * idf.get(e.getKey());
            vec.put(e.getKey(), v);
            norm += v * v;
        }
        norm = Math.sqrt(norm);
        if (norm == 0.0) {
            return Map.of();
        }
        for (Map.Entry<String, Double> e : vec.entrySet()) {
            e.setValue(e.getValue() / norm);
        }
        return vec;
    }

    /** Cosine similarity between two L2-normalized vectors (dot product). */
    public static double cosine(Map<String, Double> a, Map<String, Double> b) {
        if (a.isEmpty() || b.isEmpty()) {
            return 0.0;
        }
        Map<String, Double> small = a.size() <= b.size() ? a : b;
        Map<String, Double> large = small == a ? b : a;
        double dot = 0.0;
        for (Map.Entry<String, Double> e : small.entrySet()) {
            Double other = large.get(e.getKey());
            if (other != null) {
                dot += e.getValue() * other;
            }
        }
        return dot;
    }

    /** Terms known after fitting, for debugging and tests. */
    public Set<String> vocabulary() {
        return Set.copyOf(idf.keySet());
    }

}
