package com.karn.recommender;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * HTTP layer over {@link RecommendationApi}, built on the JDK's own
 * {@code com.sun.net.httpserver} -- no web framework dependency.
 *
 * <p>Routing lives in {@link #handle}, which is pure (method + path +
 * query + body in, status + JSON out) and is what the tests exercise;
 * {@link #start} is only the thin socket adapter around it, so the
 * suite never needs to bind a port.
 *
 * <p>Routes:
 * <ul>
 *   <li>{@code GET /health} -- status, rating count, engine names</li>
 *   <li>{@code GET /v1/recommend?user=..&n=..&engine=..} -- top-n unseen items</li>
 *   <li>{@code GET /v1/predict?user=..&item=..&engine=..} -- one prediction</li>
 *   <li>{@code POST /v1/ratings} -- {@code {"userId","itemId","rating"}}; 201</li>
 * </ul>
 * Bad input is a 400 with an {@code error} message, unknown routes
 * 404, wrong methods 405. The {@code engine} parameter defaults to
 * {@code hybrid} everywhere.
 */
public final class ApiServer {
    /** A handled response: HTTP status plus a complete JSON body. */
    public record Response(int status, String body) {
    }

    private final RecommendationApi api;
    private HttpServer server;

    public ApiServer(RecommendationApi api) {
        this.api = api;
    }

    public Response handle(String method, String path, Map<String, String> query, String body) {
        try {
            if ("/health".equals(path)) {
                requireMethod(method, "GET");
                return ok(healthJson());
            }
            if ("/v1/recommend".equals(path)) {
                requireMethod(method, "GET");
                String user = requiredQuery(query, "user");
                int n = query.containsKey("n") ? parseN(query.get("n")) : 10;
                List<ScoredItem> items = api.recommend(user, n, query.get("engine"));
                return ok(recommendJson(user, query.get("engine"), items));
            }
            if ("/v1/predict".equals(path)) {
                requireMethod(method, "GET");
                String user = requiredQuery(query, "user");
                String item = requiredQuery(query, "item");
                double score = api.predict(user, item, query.get("engine"));
                return ok("{\"userId\":" + Json.quote(user)
                        + ",\"itemId\":" + Json.quote(item)
                        + ",\"engine\":" + Json.quote(engineName(query))
                        + ",\"score\":" + format(score) + "}");
            }
            if ("/v1/ratings".equals(path)) {
                requireMethod(method, "POST");
                Map<String, String> fields = Json.parseFlatObject(body);
                String user = Json.requiredString(fields, "userId");
                String item = Json.requiredString(fields, "itemId");
                double value = Json.requiredDouble(fields, "rating");
                api.addRating(user, item, value);
                return new Response(201, "{\"userId\":" + Json.quote(user)
                        + ",\"itemId\":" + Json.quote(item)
                        + ",\"rating\":" + format(value) + "}");
            }
            return new Response(404, errorJson("not found: " + path));
        } catch (MethodNotAllowedException e) {
            return new Response(405, errorJson(e.getMessage()));
        } catch (IllegalArgumentException e) {
            return new Response(400, errorJson(e.getMessage()));
        } catch (Exception e) {
            return new Response(500, errorJson("internal error: " + e.getMessage()));
        }
    }

    /** Start serving on the given port (0 = any free port); returns the bound port. */
    public int start(int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            Map<String, String> query = parseQuery(exchange.getRequestURI().getRawQuery());
            Response response = handle(exchange.getRequestMethod(), exchange.getRequestURI().getPath(), query, body);
            byte[] bytes = response.body().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(response.status(), bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        });
        server.setExecutor(null);
        server.start();
        return server.getAddress().getPort();
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    /** Parse a raw query string ({@code a=1&b=x%20y}) into a map. */
    public static Map<String, String> parseQuery(String rawQuery) {
        Map<String, String> query = new LinkedHashMap<>();
        if (rawQuery == null || rawQuery.isBlank()) {
            return query;
        }
        for (String pair : rawQuery.split("&")) {
            int eq = pair.indexOf('=');
            String key = decode(eq < 0 ? pair : pair.substring(0, eq));
            String value = eq < 0 ? "" : decode(pair.substring(eq + 1));
            query.put(key, value);
        }
        return query;
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private String healthJson() {
        StringBuilder engines = new StringBuilder("[");
        boolean first = true;
        for (String name : api.engineNames()) {
            if (!first) {
                engines.append(",");
            }
            engines.append(Json.quote(name));
            first = false;
        }
        engines.append("]");
        return "{\"status\":\"ok\",\"ratings\":" + api.ratingCount()
                + ",\"engines\":" + engines + "}";
    }

    private String recommendJson(String user, String engine, List<ScoredItem> items) {
        StringBuilder out = new StringBuilder("{\"userId\":").append(Json.quote(user))
                .append(",\"engine\":").append(Json.quote(engineName(engine)))
                .append(",\"recommendations\":[");
        boolean first = true;
        for (ScoredItem item : items) {
            if (!first) {
                out.append(",");
            }
            out.append("{\"itemId\":").append(Json.quote(item.itemId()))
                    .append(",\"score\":").append(format(item.score())).append("}");
            first = false;
        }
        return out.append("]}").toString();
    }

    private static String engineName(Map<String, String> query) {
        return engineName(query.get("engine"));
    }

    private static String engineName(String engine) {
        return engine == null || engine.isBlank() ? RecommendationApi.DEFAULT_ENGINE : engine;
    }

    private static String requiredQuery(Map<String, String> query, String key) {
        String value = query.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("missing required query parameter: " + key);
        }
        return value;
    }

    private static int parseN(String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("n must be an integer");
        }
    }

    private static void requireMethod(String method, String expected) {
        if (!expected.equals(method)) {
            throw new MethodNotAllowedException(expected);
        }
    }

    private static Response ok(String body) {
        return new Response(200, body);
    }

    private static String errorJson(String message) {
        return "{\"error\":" + Json.quote(message == null ? "unknown error" : message) + "}";
    }

    /** Scores are rendered to 4 decimals so responses stay readable. */
    private static String format(double score) {
        return Double.toString(Math.round(score * 10000.0) / 10000.0);
    }

    /** Carries a 405 out of handle()'s 400 catch-all. */
    private static final class MethodNotAllowedException extends RuntimeException {
        MethodNotAllowedException(String expected) {
            super("method not allowed, expected " + expected);
        }
    }
}
