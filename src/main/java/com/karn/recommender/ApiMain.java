package com.karn.recommender;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Runs the recommendation HTTP API over the sample data:
 *
 * <pre>
 *   java -cp out com.karn.recommender.ApiMain [--port 8080]
 *   curl "localhost:8080/v1/recommend?user=mia"
 * </pre>
 *
 * Custom data can be pointed at with {@code --ratings <csv>} and
 * {@code --items <csv>}; both default to the bundled {@code data/}
 * files. The server keeps running until Ctrl-C.
 */
public final class ApiMain {
    private ApiMain() {
    }

    public static void main(String[] args) throws IOException {
        int port = 8080;
        Path ratingsPath = Path.of("data/sample-ratings.csv");
        Path itemsPath = Path.of("data/sample-items.csv");
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--port" -> port = Integer.parseInt(args[++i]);
                case "--ratings" -> ratingsPath = Path.of(args[++i]);
                case "--items" -> itemsPath = Path.of(args[++i]);
                default -> {
                    System.err.println("unknown argument: " + args[i]);
                    System.err.println("usage: ApiMain [--port 8080] [--ratings <csv>] [--items <csv>]");
                    System.exit(2);
                }
            }
        }
        RatingsStore store = RatingsCsv.load(ratingsPath);
        RecommendationApi api = new RecommendationApi(store, ItemCatalog.load(itemsPath).texts());
        ApiServer server = new ApiServer(api);
        int bound = server.start(port);
        System.out.println("recommender-api listening on http://localhost:" + bound
                + " (" + store.size() + " ratings loaded)");
        System.out.println("try: curl \"http://localhost:" + bound + "/v1/recommend?user=mia\"");
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
        // Block the main thread; the HttpServer runs on its own threads.
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
