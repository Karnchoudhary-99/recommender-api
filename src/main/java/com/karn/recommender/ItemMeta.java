package com.karn.recommender;

/** Static metadata for one item: id, display title, searchable description. */
public record ItemMeta(String itemId, String title, String description) {
}
