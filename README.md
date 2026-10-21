# Recommendation Engine API

A recommendation engine library + HTTP API: collaborative filtering,
content-based recommendations, and a hybrid combiner — with offline
evaluation (precision@k, recall@k, NDCG) so every recommender can be
compared honestly before it serves traffic.

## Roadmap

- [x] Project scaffold (Gradle layout, README, CI)
- [ ] Data model + in-memory ratings store
- [ ] Collaborative filtering (user/item k-NN, matrix factorisation)
- [ ] Content-based recommender (TF-IDF item profiles)
- [ ] Hybrid combiner + HTTP API
- [ ] Evaluation harness (precision@k, recall@k, NDCG), README, then public

## Usage

```bash
./gradlew test
./gradlew run
```

## License

MIT
