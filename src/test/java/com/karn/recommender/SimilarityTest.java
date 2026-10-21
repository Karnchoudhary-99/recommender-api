package com.karn.recommender;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.junit.jupiter.api.Test;

class SimilarityTest {
    @Test
    void cosineIdenticalIsOne() {
        Map<String, Double> v = Map.of("a", 3.0, "b", 4.0);
        assertEquals(1.0, Similarity.cosine(v, v), 1e-9);
    }

    @Test
    void cosineDisjointIsZero() {
        assertEquals(0.0, Similarity.cosine(Map.of("a", 5.0), Map.of("b", 5.0)), 1e-9);
    }

    @Test
    void pearsonIdenticalIsOneAndInvertedIsMinusOne() {
        Map<String, Double> a = Map.of("x", 1.0, "y", 2.0, "z", 3.0);
        Map<String, Double> b = Map.of("x", 2.0, "y", 4.0, "z", 6.0);
        Map<String, Double> c = Map.of("x", 3.0, "y", 2.0, "z", 1.0);
        assertEquals(1.0, Similarity.pearson(a, b), 1e-9);
        assertEquals(-1.0, Similarity.pearson(a, c), 1e-9);
    }

    @Test
    void pearsonNeedsTwoSharedKeys() {
        assertEquals(0.0,
                Similarity.pearson(Map.of("x", 5.0, "y", 1.0), Map.of("x", 5.0, "z", 1.0)), 1e-9);
    }
}
