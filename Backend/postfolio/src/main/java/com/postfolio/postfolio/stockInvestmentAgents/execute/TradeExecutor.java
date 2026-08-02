package com.postfolio.postfolio.stockInvestmentAgents.execute;

import com.postfolio.postfolio.stockInvestmentAgents.model.RunResult;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Simulated fills at the run's quote snapshot prices (docs/agent-trader-v2.md §7).
 * No re-fetch, so cost math exactly matches what the Risk Gate approved.
 */
@Service
public class TradeExecutor {

    public void execute(RunResult result, Map<String, Double> quotes) {
        double totalInvested = 0;
        for (Map.Entry<String, Integer> entry : result.plannedShares.entrySet()) {
            String ticker = entry.getKey();
            int shares = entry.getValue();
            double price = quotes.get(ticker);
            double cost = shares * price;

            Map<String, Double> fill = new LinkedHashMap<>();
            fill.put("shares", (double) shares);
            fill.put("price", price);
            fill.put("cost", cost);
            result.executedTrades.put(ticker, fill);
            totalInvested += cost;
        }
        result.totalInvested = round2(totalInvested);
        result.remainingAllowance = round2(result.startingAllowance - totalInvested);
        result.addTrace("executor", "ok",
                "Filled %d position(s) for $%.2f".formatted(result.executedTrades.size(), result.totalInvested),
                Map.of("executedTrades", result.executedTrades));
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
