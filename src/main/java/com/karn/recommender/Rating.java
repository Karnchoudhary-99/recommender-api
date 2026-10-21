package com.karn.recommender;

/** One explicit rating: a user scored an item. Ratings are on a 1-5 scale by convention. */
public record Rating(String userId, String itemId, double value) {
    public Rating {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId must not be blank");
        }
        if (itemId == null || itemId.isBlank()) {
            throw new IllegalArgumentException("itemId must not be blank");
        }
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            throw new IllegalArgumentException("rating value must be finite");
        }
    }
}
