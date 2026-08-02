package com.postfolio.postfolio.stockInvestmentAgents.news;

import com.postfolio.postfolio.stockInvestmentAgents.AgentUnavailableException;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NewsScoutTests {

    // Positive: headlines are formatted "headline (source)".
    @Test
    void normalizeFormatsHeadlinesWithSource() {
        List<String> result = NewsScout.normalize(List.of(
                Map.of("headline", "Fed holds rates", "source", "Reuters"),
                Map.of("headline", "Chips rally", "source", "CNBC")));
        assertEquals(List.of("Fed holds rates (Reuters)", "Chips rally (CNBC)"), result);
    }

    // Negative: missing key fails fast with a clear 503-style error.
    @Test
    void missingApiKeyThrows() {
        AgentUnavailableException e = assertThrows(AgentUnavailableException.class,
                () -> new NewsScout("").fetchHeadlines());
        assertTrue(e.getMessage().contains("FINNHUB_API_KEY"));
    }

    // Negative: entries without headlines are filtered; missing source falls back.
    @Test
    void normalizeSkipsMalformedEntries() {
        Map<String, Object> noHeadline = new HashMap<>();
        noHeadline.put("source", "X");
        Map<String, Object> noSource = new HashMap<>();
        noSource.put("headline", "Oil spikes");
        List<String> result = NewsScout.normalize(List.of(noHeadline, noSource));
        assertEquals(List.of("Oil spikes (unknown)"), result);
    }

    // Negative: payload with zero usable headlines throws.
    @Test
    void normalizeRejectsAllMalformedPayload() {
        assertThrows(AgentUnavailableException.class,
                () -> NewsScout.normalize(List.of(Map.of("source", "nowhere"))));
    }
}
