package com.postfolio.postfolio.stockInvestmentAgents.capital;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PositionSizerTests {

    private final PositionSizer sizer = new PositionSizer();

    // Positive: dollars floor down to whole shares, leftover returned as unspent.
    @Test
    void floorsDollarsToWholeShares() {
        PositionSizer.Sizing sizing = sizer.size(Map.of("NVDA", 350.0), Map.of("NVDA", 120.0));
        assertEquals(2, sizing.shares().get("NVDA"));
        assertEquals(110.0, sizing.unspent(), 0.001);
        assertTrue(sizing.rejected().isEmpty());
    }

    // Negative: too expensive for one share → skipped with reason, dollars back to cash.
    @Test
    void skipsWhenCannotAffordOneShare() {
        PositionSizer.Sizing sizing = sizer.size(Map.of("BRK", 100.0), Map.of("BRK", 700000.0));
        assertTrue(sizing.shares().isEmpty());
        assertEquals("cannot_afford_one_share", sizing.rejected().get(0).get("reason"));
        assertEquals(100.0, sizing.unspent(), 0.001);
    }

    // Negative: ticker missing from the quote snapshot is rejected defensively.
    @Test
    void rejectsTickerWithoutQuote() {
        PositionSizer.Sizing sizing = sizer.size(Map.of("GHOST", 200.0), Map.of());
        assertTrue(sizing.shares().isEmpty());
        assertEquals("no_valid_quote", sizing.rejected().get(0).get("reason"));
    }

    // Negative: empty approved book produces an empty, zero-unspent result.
    @Test
    void emptyBookYieldsEmptySizing() {
        PositionSizer.Sizing sizing = sizer.size(Map.of(), Map.of("NVDA", 120.0));
        assertTrue(sizing.shares().isEmpty());
        assertTrue(sizing.rejected().isEmpty());
        assertEquals(0.0, sizing.unspent(), 0.001);
    }
}
