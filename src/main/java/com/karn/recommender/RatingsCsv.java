package com.karn.recommender;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;

/** Minimal CSV loader: {@code userId,itemId,rating} per line. */
public final class RatingsCsv {
    private RatingsCsv() {
    }

    public static RatingsStore load(Path path) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            return parse(reader);
        }
    }

    public static RatingsStore parse(Reader reader) throws IOException {
        RatingsStore store = new RatingsStore();
        try (BufferedReader lines = new BufferedReader(reader)) {
            String line;
            int lineNo = 0;
            while ((line = lines.readLine()) != null) {
                lineNo++;
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                if (trimmed.startsWith("userId,")) {
                    continue; // header row (may follow comment lines)
                }
                String[] parts = trimmed.split(",", -1);
                if (parts.length != 3) {
                    throw new IllegalArgumentException(
                            "line " + lineNo + ": expected userId,itemId,rating, got: " + line);
                }
                double value;
                try {
                    value = Double.parseDouble(parts[2].trim());
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException(
                            "line " + lineNo + ": rating is not a number: " + parts[2].trim(), e);
                }
                store.add(parts[0].trim(), parts[1].trim(), value);
            }
        }
        return store;
    }
}
