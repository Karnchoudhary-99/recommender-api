package com.karn.recommender;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SmokeTest {
    @Test
    void scaffoldLoads() {
        assertTrue(App.class.getName().endsWith("App"));
    }
}
