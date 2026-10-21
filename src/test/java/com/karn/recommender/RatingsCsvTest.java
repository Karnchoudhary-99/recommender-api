package com.karn.recommender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.StringReader;
import org.junit.jupiter.api.Test;

class RatingsCsvTest {
    @Test
    void parsesCommentsHeaderAndRows() throws Exception {
        String csv = "# a comment\nuserId,itemId,rating\nana,dune,5\nben,dune,3\n";
        RatingsStore store = RatingsCsv.parse(new StringReader(csv));
        assertEquals(2, store.size());
        assertEquals(5.0, store.ratingsForUser("ana").get("dune"));
    }

    @Test
    void rejectsMalformedRows() {
        assertThrows(IllegalArgumentException.class,
                () -> RatingsCsv.parse(new StringReader("ana,dune\n")));
        assertThrows(IllegalArgumentException.class,
                () -> RatingsCsv.parse(new StringReader("ana,dune,high\n")));
    }
}
