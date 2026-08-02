package com.postfolio.postfolio.stockInvestmentAgents.capital;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RiskBookGateTests {

    private final RiskBookGate gate = new RiskBookGate();

    // Positive: a compliant book passes untouched with no violations.
    @Test
    void compliantBookPasses() {
        RiskBookGate.GateResult result = gate.check(
                Map.of("NVDA", 2, "AAPL", 1),
                Map.of("NVDA", 120.0, "AAPL", 200.0),
                1000, 150);
        assertTrue(result.violations().isEmpty());
        assertEquals(2, result.approvedShares().get("NVDA"));
        assertEquals(1, result.approvedShares().get("AAPL"));
    }

    // Negative: single-name weight over 35% of the starting allowance is trimmed.
    @Test
    void trimsSingleNameWeightViolation() {
        RiskBookGate.GateResult result = gate.check(
                Map.of("NVDA", 4),          // $480 > $350 cap
                Map.of("NVDA", 120.0),
                1000, 150);
        assertTrue(result.violations().contains("single_name_weight:NVDA"));
        assertEquals(2, result.approvedShares().get("NVDA")); // floor(350/120)
    }

    // Negative: more than max positions → smallest notionals dropped.
    @Test
    void dropsExcessPositions() {
        Map<String, Integer> book = new LinkedHashMap<>();
        Map<String, Double> quotes = new LinkedHashMap<>();
        String[] tickers = {"AAA", "BBB", "CCC", "DDD", "EEE", "FFF"};
        for (int i = 0; i < tickers.length; i++) {
            book.put(tickers[i], 1);
            quotes.put(tickers[i], 100.0 + i); // FFF largest, AAA smallest
        }
        RiskBookGate.GateResult result = gate.check(book, quotes, 1000, 150);
        assertEquals(RiskBookGate.MAX_POSITIONS, result.approvedShares().size());
        assertFalse(result.approvedShares().containsKey("AAA"));
    }

    // Negative: total cost over the cash floor is trimmed back under it.
    @Test
    void enforcesCashReserveFloor() {
        RiskBookGate.GateResult result = gate.check(
                Map.of("NVDA", 2, "AAPL", 1, "MSFT", 1),   // 2*300 + 340 + 330 = 1270 > 850
                Map.of("NVDA", 300.0, "AAPL", 340.0, "MSFT", 330.0),
                1000, 150);
        assertTrue(result.violations().contains("cash_floor_breached"));
        double cost = result.approvedShares().entrySet().stream()
                .mapToDouble(e -> e.getValue() * Map.of("NVDA", 300.0, "AAPL", 340.0, "MSFT", 330.0).get(e.getKey()))
                .sum();
        assertTrue(cost <= 850.0);
    }

    // Negative: invalid tickers and unquoted names are dropped outright.
    @Test
    void dropsInvalidTickers() {
        RiskBookGate.GateResult result = gate.check(
                Map.of("nvda!", 1, "GHOST", 1, "AAPL", 1),
                Map.of("AAPL", 200.0),
                1000, 150);
        assertEquals(Map.of("AAPL", 1), result.approvedShares());
        assertTrue(result.violations().stream().anyMatch(v -> v.startsWith("invalid_ticker:")));
    }
}
