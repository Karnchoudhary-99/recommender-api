package com.karn.recommender;

/** An item together with its predicted score, ordered best-first. */
public record ScoredItem(String itemId, double score) implements Comparable<ScoredItem> {
    @Override
    public int compareTo(ScoredItem other) {
        return Double.compare(other.score, this.score);
    }
}
