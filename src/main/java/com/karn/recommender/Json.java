package com.karn.recommender;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Minimal JSON helpers for the HTTP API: escaping for output, and parsing
 * of flat request objects ({@code {"userId": "mia", "rating": 4}}).
 * Deliberately small -- the API only accepts flat objects, so it does not
 * need a full parser, and adding a JSON library dependency for this
 * would be pure weight. Anything nested, trailing, or malformed is
 * rejected with {@link IllegalArgumentException}.
 */
public final class Json {
    private Json() {
    }

    /** Escape a string for inclusion inside JSON double quotes. */
    public static String escape(String value) {
        StringBuilder out = new StringBuilder(value.length() + 2);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.toString();
    }

    /** Render a string as a quoted JSON string literal. */
    public static String quote(String value) {
        return "\"" + escape(value) + "\"";
    }

    /**
     * Parse a flat JSON object into key -> raw value. String values are
     * unquoted and unescaped; number/boolean/null values are kept as
     * their literal text. Nested objects and arrays are rejected.
     */
    public static Map<String, String> parseFlatObject(String json) {
        if (json == null) {
            throw new IllegalArgumentException("request body must be a JSON object");
        }
        String text = json.trim();
        if (!text.startsWith("{") || !text.endsWith("}")) {
            throw new IllegalArgumentException("request body must be a JSON object");
        }
        Map<String, String> fields = new LinkedHashMap<>();
        int i = 1;
        int end = text.length() - 1;
        while (true) {
            i = skipWhitespace(text, i);
            if (i >= end) {
                break;
            }
            if (text.charAt(i) != '"') {
                throw new IllegalArgumentException("expected a string key at position " + i);
            }
            int keyEnd = scanString(text, i);
            String key = unescape(text.substring(i + 1, keyEnd));
            i = skipWhitespace(text, keyEnd + 1);
            if (i >= end || text.charAt(i) != ':') {
                throw new IllegalArgumentException("expected ':' after key " + key);
            }
            i = skipWhitespace(text, i + 1);
            if (i >= end) {
                throw new IllegalArgumentException("missing value for key " + key);
            }
            String value;
            if (text.charAt(i) == '"') {
                int valueEnd = scanString(text, i);
                value = unescape(text.substring(i + 1, valueEnd));
                i = valueEnd + 1;
            } else {
                int valueEnd = i;
                while (valueEnd < end && text.charAt(valueEnd) != ',') {
                    valueEnd++;
                }
                value = text.substring(i, valueEnd).trim();
                if (value.isEmpty() || value.contains("{") || value.contains("[") || value.contains("\"")) {
                    throw new IllegalArgumentException("unsupported value for key " + key);
                }
                i = valueEnd;
            }
            fields.put(key, value);
            i = skipWhitespace(text, i);
            if (i >= end) {
                break;
            }
            if (text.charAt(i) != ',') {
                throw new IllegalArgumentException("expected ',' between fields");
            }
            i++;
        }
        return fields;
    }

    /** Required string field, or an error naming the missing key. */
    public static String requiredString(Map<String, String> fields, String key) {
        String value = fields.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("missing required field: " + key);
        }
        return value;
    }

    /** Required numeric field, parsed as a double. */
    public static double requiredDouble(Map<String, String> fields, String key) {
        String value = requiredString(fields, key);
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("field " + key + " must be a number");
        }
    }

    private static int skipWhitespace(String text, int i) {
        while (i < text.length() && Character.isWhitespace(text.charAt(i))) {
            i++;
        }
        return i;
    }

    /** Index of the closing quote of the string starting at {@code start}. */
    private static int scanString(String text, int start) {
        int i = start + 1;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (c == '\\') {
                i += 2;
            } else if (c == '"') {
                return i;
            } else {
                i++;
            }
        }
        throw new IllegalArgumentException("unterminated string");
    }

    private static String unescape(String raw) {
        if (!raw.contains("\\")) {
            return raw;
        }
        StringBuilder out = new StringBuilder(raw.length());
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == '\\' && i + 1 < raw.length()) {
                char next = raw.charAt(++i);
                switch (next) {
                    case '"' -> out.append('"');
                    case '\\' -> out.append('\\');
                    case 'n' -> out.append('\n');
                    case 'r' -> out.append('\r');
                    case 't' -> out.append('\t');
                    default -> out.append(next);
                }
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }
}
