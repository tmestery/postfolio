package com.postfolio.postfolio.stockInvestmentAgents.marketdata;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlpacaRestClientTests {

    private final AlpacaRestClient client = new AlpacaRestClient(new AlpacaConfig("", "", "iex", "https://data.alpaca.markets"));

    // Positive: quote mid from bid/ask.
    @Test
    void parsesQuoteMidFromSnapshot() {
        Map<String, Double> marks = client.parseSnapshots("""
                {"AAPL":{"latestQuote":{"bp":100,"ap":102}}}
                """);
        assertEquals(101.0, marks.get("AAPL"), 0.001);
    }

    // Negative: missing keys yield empty map.
    @Test
    void invalidJsonReturnsEmpty() {
        assertTrue(client.parseSnapshots("not-json").isEmpty());
    }

    // Edge: falls back to latest trade price.
    @Test
    void fallsBackToTradePrice() {
        Map<String, Double> marks = client.parseSnapshots("""
                {"TSLA":{"latestTrade":{"p":250.5}}}
                """);
        assertEquals(250.5, marks.get("TSLA"), 0.001);
    }

    // Failure: zero/negative prices omitted.
    @Test
    void skipsNonPositivePrices() {
        Map<String, Double> marks = client.parseSnapshots("""
                {"ZZ":{"latestTrade":{"p":0}}}
                """);
        assertTrue(marks.isEmpty());
    }
}
