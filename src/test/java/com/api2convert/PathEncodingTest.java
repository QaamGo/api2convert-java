package com.api2convert;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Dynamic path segments (job/preset IDs, stats date/filter) come from the caller and are
 * interpolated into the request path. A value containing {@code "/"}, {@code "?"} or {@code "#"}
 * must be percent-encoded so it cannot traverse to another resource or start the query/fragment;
 * the fixed {@code "/"} separators between segments stay literal.
 */
class PathEncodingTest extends A2CTestCase {

    @Test
    void jobIdSegmentIsEncoded() {
        http.addJson(200, "{\"id\":\"j\"}");

        client().jobs().get("a/b?c#d e");

        assertTrue(requestAt(0).uri().endsWith("/jobs/a%2Fb%3Fc%23d%20e"), requestAt(0).uri());
    }

    @Test
    void presetIdSegmentIsEncoded() {
        http.addJson(200, "{\"id\":\"p\"}");

        client().presets().get("x/y#z");

        assertTrue(requestAt(0).uri().endsWith("/presets/x%2Fy%23z"), requestAt(0).uri());
    }

    @Test
    void statsSegmentsAreEncodedButSeparatorsStayLiteral() {
        http.addJson(200, "{}");

        client().stats().day("2026-07-06", "a/b?c");

        assertTrue(requestAt(0).uri().endsWith("/stats/day/2026-07-06/a%2Fb%3Fc"), requestAt(0).uri());
    }
}
