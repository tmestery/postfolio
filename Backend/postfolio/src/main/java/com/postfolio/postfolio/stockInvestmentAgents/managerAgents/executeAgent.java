package com.postfolio.postfolio.stockInvestmentAgents.managerAgents;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.HashMap;
import java.util.Map;

// Agents that utilizes manager which uses agents to collect stock investment data
@Service
public class executeAgent {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String apiKey = System.getenv("FINNHUB_API_KEY");
    private boolean executed = false;

    // Returns a map with stock, shares, and price as well as the allowance (remaining + invested amount)
    public synchronized Map<String, Object> executeTrades(Map<String, Double> stockShares, double allowance) {
        if (executed) {
            return Map.of(
                    "error", "Trades already completed..."
            );
        }
        executed = true;

        Map<String, Object> response = new HashMap<>();
        Map<String, Map<String, Double>> executedTrades = new HashMap<>();

        double totalInvested = 0;

        // Adding to the map
        for (Map.Entry<String, Double> entry : stockShares.entrySet()) {
            String symbol = entry.getKey();
            double shares = entry.getValue();

            if (shares <= 0) continue;

            double price = fetchCurrentPrice(symbol);
            double cost = shares * price;

            if (cost > allowance) continue;

            allowance -= cost;
            totalInvested += cost;

            // Map details here
            Map<String, Double> tradeDetails = new HashMap<>();
            tradeDetails.put("shares", shares);
            tradeDetails.put("price", price);
            tradeDetails.put("cost", cost);

            executedTrades.put(symbol, tradeDetails);
        }

        response.put("executedTrades", executedTrades);
        response.put("totalInvested", totalInvested);
        response.put("remainingAllowance", allowance);

        return response;
    }

    // Gets the current price from the finnhub api
    private double fetchCurrentPrice(String symbol) {
        String url = "https://finnhub.io/api/v1/quote?symbol=" + symbol + "&token=" + apiKey;
        Map<String, Object> result = restTemplate.getForObject(url, Map.class);

        // Confirms that the result is there
        if (result != null && result.get("c") != null) {
            return ((Number) result.get("c")).doubleValue();
        }

        return 0;
    }
}