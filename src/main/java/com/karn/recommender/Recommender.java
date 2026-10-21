package com.karn.recommender;

import java.util.List;

/** A model that predicts ratings and ranks unseen items for a user. */
public interface Recommender {
    /** Predicted rating for (userId, itemId); falls back to means for cold users/items. */
    double predict(String userId, String itemId);

    /** Top-n items the user has not rated yet, best first. */
    List<ScoredItem> recommend(String userId, int n);
}
