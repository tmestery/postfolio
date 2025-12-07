package com.postfolio.postfolio.stockInvestmentAgents.analyzerAgents;

import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class costAnalysisAgent {
    private double investmentCost;
    private final String OLLAMA_URL = "http://localhost:11434/api/generate";
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
        private final String apiKey = System.getenv("FINNHUB_API_KEY");

    // Determine number of shares to buy based on the stock
    public double determineNumShares(String bestStockOption, double allowance) {
        try {
            // Build the prompt dynamically for determing good share amount to purchase of stock
            String prompt = "You are a professional at predicting stock anaylsis. " + 
            "Here is the stock that you're going to be investing in: " + bestStockOption + 
            " You have a balance of the following: $" + allowance + " Use this information to make " +
            "an educated decision on the amount of money to invest into this stock.";

            // Build request body for Ollama
            Map<String, Object> requestBody = Map.of("model", "llama3", "stream", false, "prompt", prompt);

            // Send POST request
            String responseString = restTemplate.postForObject(OLLAMA_URL, requestBody, String.class);

            // Parse Ollama response
            Map<String, Object> aiResult = objectMapper.readValue(responseString, Map.class);
            double amountToInvest = (double) aiResult.get("response");

            return sharesToCost(amountToInvest, bestStockOption);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return 0;
    }

    // Make API Call to transition number of shares to actual price:
    public double sharesToCost(double amountToInvest, String stockSymbol) {
        // Make the API Call
        String url = "https://finnhub.io/api/v1//quote?symbol=" + stockSymbol + "token=" + apiKey;

        return 0;
    }

    // Updates the allowance, subtracting cost of the investment
    public double updateAllowance(double allowance) {
        return allowance - investmentCost;
    }

    public void setInvestmentCost(double investmentCost) {
        this.investmentCost = investmentCost;
    }

    public double getInvestmentCost() {
        return investmentCost;
    }
}