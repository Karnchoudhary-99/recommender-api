#!/bin/bash
# Tour of the recommender-api HTTP service. Start the server first:
#   javac -d out $(find src/main/java -name '*.java')
#   java -cp out com.karn.recommender.ApiMain --port 8080
# Then run this script (it assumes port 8080).
set -e
BASE="${BASE:-http://localhost:8080}"

echo "== health =="
curl -s "$BASE/health"; echo

echo "== hybrid recommendations for mia (default engine) =="
curl -s "$BASE/v1/recommend?user=mia"; echo

echo "== content-based recommendations for mia =="
curl -s "$BASE/v1/recommend?user=mia&engine=content"; echo

echo "== predict arrival for mia =="
curl -s "$BASE/v1/predict?user=mia&item=arrival"; echo

echo "== post a rating, then re-predict =="
curl -s -X POST "$BASE/v1/ratings" \
  -H 'Content-Type: application/json' \
  -d '{"userId": "mia", "itemId": "arrival", "rating": 5}'; echo
curl -s "$BASE/v1/predict?user=mia&item=arrival&engine=itemknn"; echo

echo "== error cases (400 unknown engine, 404 unknown route) =="
curl -s "$BASE/v1/recommend?user=mia&engine=nope"; echo
curl -s "$BASE/v1/nope"; echo
