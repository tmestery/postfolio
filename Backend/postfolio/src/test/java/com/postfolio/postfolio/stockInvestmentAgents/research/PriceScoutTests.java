package com.postfolio.postfolio.stockInvestmentAgents.research;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PriceScoutTests {

    @Test
    void parsesYahooChartJson() {
        PageFetcher fetcher = url -> Optional.of("""
                {"chart":{"result":[{"meta":{"regularMarketPrice":123.45,"previousClose":120}}]}}
                """);
        Map<String, Double> snap = new PriceScout(fetcher).snapshot(List.of("AAPL"));
        assertEquals(123.45, snap.get("AAPL"), 0.001);
    }

    @Test
    void missingQuoteOmitsTicker() {
        PageFetcher fetcher = url -> Optional.empty();
        assertTrue(new PriceScout(fetcher).snapshot(List.of("GHOST")).isEmpty());
    }

    @Test
    void blankTickersIgnored() {
        PageFetcher fetcher = url -> Optional.of("{\"chart\":{\"result\":[{\"meta\":{\"regularMarketPrice\":10}}]}}");
        assertTrue(new PriceScout(fetcher).snapshot(java.util.Arrays.asList(" ", null, "")).isEmpty());
    }

    @Test
    void rejectsNonPositivePrices() {
        PageFetcher fetcher = url -> Optional.of("{\"chart\":{\"result\":[{\"meta\":{\"regularMarketPrice\":0}}]}}");
        assertTrue(new PriceScout(fetcher).snapshot(List.of("ZZZZ")).isEmpty());
    }
}
