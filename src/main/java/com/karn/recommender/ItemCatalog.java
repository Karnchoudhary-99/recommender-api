package com.karn.recommender;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Item metadata loader: CSV with {@code itemId,title,description} columns.
 * Comments ({@code #...}) and a header row are tolerated, and quoted fields
 * may contain commas -- the same lenient shape as {@link RatingsCsv}.
 */
public record ItemCatalog(List<ItemMeta> items) {

    /** Loads a catalog; item texts are {@code title + " " + description}. */
    public static ItemCatalog load(Path path) throws IOException {
        List<ItemMeta> metas = new ArrayList<>();
        for (String line : Files.readAllLines(path)) {
            String trimmed = line.strip();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            List<String> fields = splitCsv(trimmed);
            if (fields.size() < 3) {
                continue;
            }
            String id = fields.get(0).strip();
            if (id.equals("itemId")) {
                continue; // header row
            }
            metas.add(new ItemMeta(id, fields.get(1).strip(), fields.get(2).strip()));
        }
        return new ItemCatalog(List.copyOf(metas));
    }

    /** itemId -> searchable text (title plus description). */
    public Map<String, String> texts() {
        Map<String, String> texts = new LinkedHashMap<>();
        for (ItemMeta meta : items) {
            texts.put(meta.itemId(), meta.title() + " " + meta.description());
        }
        return texts;
    }

    /** Minimal CSV split honouring double-quoted fields and "" escapes. */
    static List<String> splitCsv(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (quoted) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        cur.append('"');
                        i++;
                    } else {
                        quoted = false;
                    }
                } else {
                    cur.append(c);
                }
            } else if (c == '"') {
                quoted = true;
            } else if (c == ',') {
                fields.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        fields.add(cur.toString());
        return fields;
    }
}
