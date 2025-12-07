package com.postfolio.postfolio.stockInvestmentAgents.analyzerAgents;

import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

@Service
public class costAnalysisAgent {
    private double investmentCost;
    private final String OLLAMA_URL = "http://localhost:11434/api/generate";
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String apiKey = System.getenv("FINNHUB_API_KEY");

    // Determine number of shares to buy based on the specific stock
    public double determineNumShares(String bestStockOption, double allowance) {
        try {
            String prompt = "You are a professional stock analyst. " +
                    "The stock you are considering for investment is: " + bestStockOption + ". " +
                    "Your available balance for investing is $" + allowance + ". " +
                    "Based on this information, provide ONLY a numeric recommendation for how much money to invest in this stock. " +
                    "Do NOT include any explanations or text, return only a number.";

            // Build request body for Ollama + sned POST
            Map<String, Object> requestBody = Map.of("model", "llama3", "stream", false, "prompt", prompt);
            String responseString = restTemplate.postForObject(OLLAMA_URL, requestBody, String.class);

            // Parse the response string using to get the first number out of it
            Pattern pattern = Pattern.compile("\\d+(\\.\\d+)?");
            Matcher matcher = pattern.matcher(responseString);

            double amountToInvest = 0;
            if (matcher.find()) {
                amountToInvest = Double.parseDouble(matcher.group());
            } else {
                System.out.println("No numeric value found in AI response, defaulting to 0");
            }

            // Convert $ to # shares using API Finnhub
            return sharesToCost(amountToInvest, bestStockOption);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    // Make API Call to transition number of shares to actual price:
    public double sharesToCost(double amountToInvest, String stockSymbol) {
        try {
            String url = "https://finnhub.io/api/v1/quote?symbol=" + stockSymbol + "&token=" + apiKey;
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response != null && response.get("c") != null) {
                double currentPrice = ((Number) response.get("c")).doubleValue();
                if (currentPrice <= 0) {
                    System.out.println("Warning: Current price for " + stockSymbol + " is invalid. Returning 0 shares.");
                    return 0;
                }

                // Calculate number of shares to buy + round to nearest whole
                double sharesToBuy = amountToInvest / currentPrice;
                sharesToBuy = Math.ceil(sharesToBuy);

                // If sharesToBuy is 0, don't invest
                if (sharesToBuy < 1) {
                    return 0;
                }

                this.investmentCost = sharesToBuy * currentPrice;
                return sharesToBuy;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    // Updates the allowance, subtracting cost of the investment
    public double updateAllowance(double allowance) {
        return allowance - investmentCost;
    }

    // setter
    public void setInvestmentCost(double investmentCost) {
        this.investmentCost = investmentCost;
    }

    // getter
    public double getInvestmentCost() {
        return investmentCost;
    }
}