package com.postfolio.postfolio.stockInvestmentAgents.portfolio;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** API DTO for the house paper book (docs/agent-trader-v4.md §8). */
public class PortfolioSnapshot {

    public double startingCashSeed;
    public double cash;
    public double investedCost;
    public double holdingsValue;
    public double equity;
    public double totalPnl;
    public double totalPnlPct;
    public double realizedPnl;
    public double unrealizedPnl;
    public Instant asOf = Instant.now();
    public boolean marksStale;
    public String dataFeed = "iex";
    public List<PositionView> positions = new ArrayList<>();

    public static class PositionView {
        public String ticker;
        public double shares;
        public double avgCost;
        public double costBasis;
        public Double markPrice;
        public double marketValue;
        public double unrealizedPnl;
        public Double unrealizedPnlPct;
        public Instant markedAt;
        public boolean live;
    }

    public Map<String, Object> toSummary() {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("equity", round2(equity));
        summary.put("totalPnl", round2(totalPnl));
        summary.put("totalPnlPct", round4(totalPnlPct));
        summary.put("cash", round2(cash));
        summary.put("holdingsValue", round2(holdingsValue));
        return summary;
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private static double round4(double v) {
        return Math.round(v * 10_000.0) / 10_000.0;
    }
}
