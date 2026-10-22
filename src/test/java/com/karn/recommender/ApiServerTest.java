package com.karn.recommender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ApiServerTest {
    private ApiServer server;

    @BeforeEach
    void setUp() throws IOException {
        RatingsStore store = RatingsCsv.load(Path.of("data/sample-ratings.csv"));
        RecommendationApi api = new RecommendationApi(store,
                ItemCatalog.load(Path.of("data/sample-items.csv")).texts());
        server = new ApiServer(api);
    }

    @Test
    void healthReportsStatusAndRatingCount() {
        ApiServer.Response response = server.handle("GET", "/health", Map.of(), "");
        assertEquals(200, response.status());
        assertTrue(response.body().contains("\"status\":\"ok\""));
        assertTrue(response.body().contains("\"ratings\":36"));
        assertTrue(response.body().contains("\"hybrid\""));
    }

    @Test
    void recommendDefaultsToHybridAndRanksArrivalFirstForMia() {
        ApiServer.Response response = server.handle("GET", "/v1/recommend",
                Map.of("user", "mia", "n", "3"), "");
        assertEquals(200, response.status());
        assertTrue(response.body().contains("\"engine\":\"hybrid\""));
        assertTrue(response.body().indexOf("arrival") < response.body().indexOf("step-brothers"));
    }

    @Test
    void recommendHonoursEngineAndRejectsBadInput() {
        ApiServer.Response content = server.handle("GET", "/v1/recommend",
                Map.of("user", "mia", "engine", "content"), "");
        assertEquals(200, content.status());
        assertTrue(content.body().contains("\"engine\":\"content\""));

        assertEquals(400, server.handle("GET", "/v1/recommend", Map.of(), "").status());
        assertEquals(400, server.handle("GET", "/v1/recommend",
                Map.of("user", "mia", "n", "abc"), "").status());
        assertEquals(400, server.handle("GET", "/v1/recommend",
                Map.of("user", "mia", "engine", "nope"), "").status());
    }

    @Test
    void predictReturnsScoreAndPostRatingReturns201ThenAffectsPredictions() {
        ApiServer.Response predict = server.handle("GET", "/v1/predict",
                Map.of("user", "mia", "item", "arrival"), "");
        assertEquals(200, predict.status());
        assertTrue(predict.body().contains("\"itemId\":\"arrival\""));

        ApiServer.Response posted = server.handle("POST", "/v1/ratings", Map.of(),
                "{\"userId\": \"mia\", \"itemId\": \"arrival\", \"rating\": 5}");
        assertEquals(201, posted.status());
        assertTrue(posted.body().contains("\"rating\":5.0"));

        ApiServer.Response after = server.handle("GET", "/v1/predict",
                Map.of("user", "mia", "item", "arrival", "engine", "itemknn"), "");
        assertTrue(after.body().contains("\"score\":5.0"));

        assertEquals(400, server.handle("POST", "/v1/ratings", Map.of(),
                "{\"userId\": \"mia\", \"itemId\": \"arrival\", \"rating\": 9}").status());
        assertEquals(400, server.handle("POST", "/v1/ratings", Map.of(), "not json").status());
    }

    @Test
    void unknownRoutes404AndWrongMethods405() {
        assertEquals(404, server.handle("GET", "/v1/nope", Map.of(), "").status());
        assertEquals(405, server.handle("DELETE", "/health", Map.of(), "").status());
        assertEquals(405, server.handle("GET", "/v1/ratings", Map.of(), "").status());
    }

    @Test
    void queryParsingDecodesPlusAndPercentEncoding() {
        Map<String, String> query = ApiServer.parseQuery("user=ann%20lee&n=2&flag");
        assertEquals("ann lee", query.get("user"));
        assertEquals("2", query.get("n"));
        assertEquals("", query.get("flag"));
        assertEquals(Map.of(), ApiServer.parseQuery(null));
    }
}
