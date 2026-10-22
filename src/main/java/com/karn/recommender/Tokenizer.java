package com.karn.recommender;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Minimal English tokenizer for item text: lowercases, splits on
 * non-alphanumeric runs, drops stop words and single characters.
 * Kept dependency-free on purpose -- the TF-IDF layer needs tokens,
 * not a full NLP pipeline.
 */
public final class Tokenizer {
    private static final Set<String> STOP_WORDS = Set.of(
            "a", "an", "the", "and", "or", "of", "to", "in", "on", "for",
            "with", "is", "are", "was", "were", "it", "its", "by", "at",
            "as", "from", "that", "this", "who", "his", "her", "their");

    private Tokenizer() {
    }

    public static List<String> tokenize(String text) {
        List<String> tokens = new ArrayList<>();
        if (text == null) {
            return tokens;
        }
        for (String raw : text.toLowerCase().split("[^a-z0-9]+")) {
            if (raw.length() > 1 && !STOP_WORDS.contains(raw)) {
                tokens.add(raw);
            }
        }
        return tokens;
    }
}
