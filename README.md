# Recommendation Engine API

A recommendation engine library + HTTP API: collaborative filtering,
content-based recommendations, and a hybrid combiner — with offline
evaluation (precision@k, recall@k, NDCG) so every recommender can be
compared honestly before it serves traffic.

## Roadmap

- [x] Project scaffold (Gradle layout, README, CI)
- [x] Data model + in-memory ratings store
- [x] Collaborative filtering (user/item k-NN, matrix factorisation)
- [x] Content-based recommender (TF-IDF item profiles)
- [x] Hybrid combiner
- [x] HTTP API
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

## Content-based and hybrid

| Piece | What it does |
| --- | --- |
| `Tokenizer` | Lowercases, splits on non-alphanumerics, drops stop words |
| `TfidfVectorizer` | Smoothed IDF, L2-normalized vectors, cosine similarity |
| `ItemCatalog` | `itemId,title,description` CSV loader (comments, header, quoted commas tolerated) |
| `ContentRecommender` | User profile = rating-weighted average of liked items' TF-IDF vectors; unseen items scored by cosine, mapped to the 1-5 scale |
| `HybridRecommender` | Weighted average of component predictions; weights normalized, cold users/items fall back to component means |

Item metadata lives in `data/sample-items.csv` (title + description
keywords per item). The demo prints content and hybrid lines alongside
the CF engines: content also ranks `arrival` first for `mia` (3.29 vs
3.00 for `step-brothers`), and a 70/30 user-kNN/content hybrid keeps
`arrival` on top.

## HTTP API

Zero-dependency, on the JDK's built-in `HttpServer` — no framework.
`ApiServer.handle()` is pure (method + path + query + body in, status
+ JSON out), so the routing is unit-tested without binding a port;
`start()` is only the thin socket adapter.

```bash
javac -d out $(find src/main/java -name '*.java')
java -cp out com.karn.recommender.ApiMain --port 8080
bash examples/api.sh   # curl tour of every route
```

| Route | What it does |
| --- | --- |
| `GET /health` | Status, loaded rating count, engine names |
| `GET /v1/recommend?user=mia&n=5&engine=hybrid` | Top-n unseen items, best first (`engine` defaults to `hybrid`) |
| `GET /v1/predict?user=mia&item=arrival&engine=itemknn` | One predicted rating |
| `POST /v1/ratings` | `{"userId","itemId","rating"}` (1-5); 201. Replaces a previous rating for the same pair, and item-kNN's similarity cache is rebuilt so the write is visible to the next request |

Engines: `userknn`, `itemknn`, `content`, `hybrid` (70% user-kNN /
30% content). Bad input is a 400 with an `error` message, unknown
routes 404, wrong methods 405.

## License

MIT
