package com.karn.recommender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class TokenizerTest {
    @Test
    void lowercasesSplitsAndDropsStopWords() {
        assertEquals(List.of("science", "fiction", "desert", "planet"),
                Tokenizer.tokenize("Science Fiction: the desert planet!"));
    }

    @Test
    void nullAndEmptyYieldNoTokens() {
        assertTrue(Tokenizer.tokenize(null).isEmpty());
        assertTrue(Tokenizer.tokenize("").isEmpty());
        assertTrue(Tokenizer.tokenize("a the and").isEmpty());
    }
}
