package com.postfolio.postfolio.stockInvestmentAgents.news;

import com.postfolio.postfolio.stockInvestmentAgents.AgentUnavailableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Fetches one Finnhub quote snapshot per run (docs/agent-trader-v2.md §5.5b).
 * Tickers without a positive price are simply absent from the snapshot.
 */
@Service
public class QuoteService {

    private final String apiKey;
    private final RestTemplate restTemplate = new RestTemplate();

    public QuoteService(@Value("${FINNHUB_API_KEY:}") String apiKey) {
        this.apiKey = apiKey;
    }

    public Map<String, Double> snapshot(List<String> tickers) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new AgentUnavailableException("FINNHUB_API_KEY is not configured on the server");
        }
        Map<String, Double> snapshot = new LinkedHashMap<>();
        for (String ticker : tickers) {
            double price = fetchPrice(ticker);
            if (price > 0) {
                snapshot.put(ticker, price);
            }
        }
        return snapshot;
    }

    private double fetchPrice(String symbol) {
        try {
            String url = "https://finnhub.io/api/v1/quote?symbol=" + symbol + "&token=" + apiKey;
            @SuppressWarnings("unchecked")
            Map<String, Object> result = restTemplate.getForObject(url, Map.class);
            if (result != null && result.get("c") instanceof Number price) {
                return price.doubleValue();
            }
            return 0;
        } catch (Exception e) {
            return 0;
        }
    }
}
