# Recommendation Engine API

A recommendation engine library + HTTP API: collaborative filtering,
content-based recommendations, and a hybrid combiner — with offline
evaluation (precision@k, recall@k, NDCG) so every recommender can be
compared honestly before it serves traffic.

## Roadmap

- [x] Project scaffold (Gradle layout, README, CI)
- [x] Data model + in-memory ratings store
- [x] Collaborative filtering (user/item k-NN, matrix factorisation)
- [ ] Content-based recommender (TF-IDF item profiles)
- [ ] Hybrid combiner + HTTP API
- [ ] Evaluation harness (precision@k, recall@k, NDCG), README, then public

## Usage

```bash
./gradlew test
./gradlew run
```

## Collaborative filtering

The library core, in `com.karn.recommender`:

| Piece | What it does |
| --- | --- |
| `RatingsStore` | In-memory ratings indexed by user and by item; means + re-rating replacement |
| `Similarity` | Cosine similarity and Pearson correlation over sparse rating maps |
| `UserKnnRecommender` | Pearson neighbours, mean-centred weighted prediction |
| `ItemKnnRecommender` | Cosine item-item similarities (cached), weighted prediction |
| `MatrixFactorization` | Biased SGD factorisation, seeded for reproducibility, `rmse()` helper |
| `RatingsCsv` | `userId,itemId,rating` loader (comments + header tolerated) |
| `CfDemo` | Runnable demo over `data/sample-ratings.csv` |

Run the demo without Gradle (JDK 17+):

```bash
javac -d out $(find src/main/java -name '*.java')
java -cp out com.karn.recommender.CfDemo
```

On the bundled sample data (two taste clusters: sci-fi vs comedy fans),
all three engines rank `arrival` first for sci-fi fan `mia`, and matrix
factorisation fits the training ratings at RMSE 0.02.

## License

MIT
