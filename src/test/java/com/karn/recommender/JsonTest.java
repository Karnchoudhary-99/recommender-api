package com.karn.recommender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import org.junit.jupiter.api.Test;

class JsonTest {

    @Test
    void escapesQuotesBackslashesAndControlChars() {
        assertEquals("a\\\"b\\\\c\\nd", Json.escape("a\"b\\c\nd"));
        assertEquals("\"x\"", Json.quote("x"));
    }

    @Test
    void parsesFlatObjectWithStringsAndNumbers() {
        Map<String, String> fields = Json.parseFlatObject("{\"userId\": \"mia\", \"itemId\":\"arrival\", \"rating\": 4.5}");
        assertEquals("mia", fields.get("userId"));
        assertEquals("arrival", fields.get("itemId"));
        assertEquals("4.5", fields.get("rating"));
    }

    @Test
    void parsesEmptyObjectAndUnescapesStrings() {
        assertEquals(Map.of(), Json.parseFlatObject("  {}  "));
        Map<String, String> fields = Json.parseFlatObject("{\"userId\": \"a\\\"b\"}");
        assertEquals("a\"b", fields.get("userId"));
    }

    @Test
    void rejectsNonObjectsNestedValuesAndUnterminatedStrings() {
        assertThrows(IllegalArgumentException.class, () -> Json.parseFlatObject("[1, 2]"));
        assertThrows(IllegalArgumentException.class, () -> Json.parseFlatObject("{\"a\": {\"b\": 1}}"));
        assertThrows(IllegalArgumentException.class, () -> Json.parseFlatObject("{\"a\": \"oops}"));
        assertThrows(IllegalArgumentException.class, () -> Json.parseFlatObject("not json"));
    }

    @Test
    void requiredHelpersNameMissingKeysAndRejectBadNumbers() {
        Map<String, String> fields = Map.of("rating", "high");
        assertThrows(IllegalArgumentException.class, () -> Json.requiredString(fields, "userId"));
        assertThrows(IllegalArgumentException.class, () -> Json.requiredDouble(fields, "rating"));
        assertEquals(4.0, Json.requiredDouble(Map.of("rating", "4"), "rating"));
    }
}
