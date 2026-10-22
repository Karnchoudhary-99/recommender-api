package com.karn.recommender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TfidfVectorizerTest {
    @Test
    void vectorizeBeforeFitThrows() {
        assertThrows(IllegalStateException.class,
                () -> new TfidfVectorizer().vectorize("hello world"));
    }

    @Test
    void vectorsAreL2NormalizedAndRareTermsWeighMore() {
        TfidfVectorizer v = new TfidfVectorizer();
        v.fit(List.of("science fiction desert", "science fiction city", "comedy party"));
        Map<String, Double> vec = v.vectorize("science fiction desert");
        double norm = Math.sqrt(vec.values().stream().mapToDouble(x -> x * x).sum());
        assertEquals(1.0, norm, 1e-9);
        // "desert" appears once, "science" twice: rarer term gets the bigger weight.
        assertTrue(vec.get("desert") > vec.get("science"));
        assertEquals(1.0, TfidfVectorizer.cosine(vec, vec), 1e-9);
        assertEquals(0.0,
                TfidfVectorizer.cosine(vec, v.vectorize("comedy party")), 1e-9);
    }

    @Test
    void unknownTermsAreIgnored() {
        TfidfVectorizer v = new TfidfVectorizer();
        v.fit(List.of("science fiction"));
        assertTrue(v.vectorize("totally unknown words").isEmpty());
    }
}
