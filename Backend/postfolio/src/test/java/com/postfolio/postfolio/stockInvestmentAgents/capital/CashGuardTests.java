package com.postfolio.postfolio.stockInvestmentAgents.capital;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CashGuardTests {

    private final CashGuard guard = new CashGuard();

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> reviews(Map<String, Object> report) {
        return (List<Map<String, Object>>) report.get("reviews");
    }

    // Positive: a proposal comfortably under deployable capital passes.
    @Test
    void approvesProposalWithinBudget() {
        Map<String, Object> report = guard.review(
                List.of(new AllocationProposal("balanced", Map.of("NVDA", 400.0), "spread")),
                1000, 150);
        Map<String, Object> review = reviews(report).get(0);
        assertFalse((Boolean) review.get("vetoSpend"));
        assertTrue(((String) review.get("note")).startsWith("OK"));
    }

    // Negative: spending past the reserve floor is vetoed.
    @Test
    void vetoesOverdeployment() {
        Map<String, Object> report = guard.review(
                List.of(new AllocationProposal("aggressive", Map.of("NVDA", 950.0), "all in")),
                1000, 150);
        Map<String, Object> review = reviews(report).get(0);
        assertTrue((Boolean) review.get("vetoSpend"));
        assertTrue(((String) review.get("note")).startsWith("VETO"));
    }

    // Negative: skimming the floor gets a caution, not a veto.
    @Test
    void cautionsWhenSkimmingReserve() {
        Map<String, Object> report = guard.review(
                List.of(new AllocationProposal("aggressive", Map.of("NVDA", 840.0), "close")),
                1000, 150);
        Map<String, Object> review = reviews(report).get(0);
        assertFalse((Boolean) review.get("vetoSpend"));
        assertTrue(((String) review.get("note")).startsWith("Caution"));
    }

    // Negative: no proposals still yields a well-formed report.
    @Test
    void emptyProposalListYieldsEmptyReviews() {
        Map<String, Object> report = guard.review(List.of(), 1000, 150);
        assertEquals(850.0, (Double) report.get("deployableCapital"), 0.001);
        assertTrue(reviews(report).isEmpty());
    }
}
