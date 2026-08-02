package com.postfolio.postfolio.stockInvestmentAgents.capital;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pure-code conversion of approved dollar notionals into whole shares using
 * the run's quote snapshot (docs/agent-trader-v2.md §6.8). Never re-fetches prices.
 */
@Service
public class PositionSizer {

    public record Sizing(Map<String, Integer> shares, List<Map<String, Object>> rejected, double unspent) {}

    public Sizing size(Map<String, Double> approvedNotionals, Map<String, Double> quotes) {
        Map<String, Integer> shares = new LinkedHashMap<>();
        List<Map<String, Object>> rejected = new ArrayList<>();
        double unspent = 0;

        for (Map.Entry<String, Double> entry : approvedNotionals.entrySet()) {
            String ticker = entry.getKey();
            double dollars = entry.getValue();
            Double price = quotes.get(ticker);
            if (price == null || price <= 0) {
                // Should not happen post-investability-gate; treat defensively.
                rejected.add(Map.of("ticker", ticker, "dollars", dollars, "reason", "no_valid_quote"));
                unspent += dollars;
                continue;
            }
            int wholeShares = (int) Math.floor(dollars / price);
            if (wholeShares <= 0) {
                rejected.add(Map.of("ticker", ticker, "dollars", dollars, "reason", "cannot_afford_one_share"));
                unspent += dollars;
                continue;
            }
            shares.put(ticker, wholeShares);
            unspent += dollars - wholeShares * price;
        }
        return new Sizing(shares, rejected, unspent);
    }
}
