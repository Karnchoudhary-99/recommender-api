package com.karn.recommender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ItemCatalogTest {
    @Test
    void loadsHeaderCommentsAndQuotedCommas(@TempDir Path dir) throws Exception {
        Path csv = dir.resolve("items.csv");
        Files.writeString(csv, "# comment\nitemId,title,description\n"
                + "dune,Dune,\"desert, spice planet\"\n"
                + "matrix,The Matrix,science fiction hacker\n");
        ItemCatalog catalog = ItemCatalog.load(csv);
        assertEquals(2, catalog.items().size());
        assertEquals("Dune", catalog.items().get(0).title());
        assertEquals("desert, spice planet", catalog.items().get(0).description());
        Map<String, String> texts = catalog.texts();
        assertTrue(texts.get("dune").contains("Dune"));
        assertTrue(texts.get("dune").contains("spice"));
    }

    @Test
    void bundledSampleCatalogCoversEveryRatedItem() throws Exception {
        ItemCatalog catalog = ItemCatalog.load(Path.of("data/sample-items.csv"));
        assertEquals(7, catalog.items().size());
        assertTrue(catalog.texts().keySet()
                .containsAll(List.of("dune", "matrix", "blade-runner", "arrival",
                        "superbad", "hangover", "step-brothers")));
    }
}
