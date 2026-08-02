package com.postfolio.postfolio.stockInvestmentAgents.news;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.postfolio.postfolio.stockInvestmentAgents.AgentUnavailableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Fetches and normalizes Finnhub market news (docs/agent-trader-v2.md §5.2). */
@Service
public class NewsScout {

    static final int MAX_HEADLINES = 60;

    private final String apiKey;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public NewsScout(@Value("${FINNHUB_API_KEY:}") String apiKey) {
        this.apiKey = apiKey;
    }

    /** Returns up to MAX_HEADLINES entries formatted as "headline (source)". */
    public List<String> fetchHeadlines() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new AgentUnavailableException("FINNHUB_API_KEY is not configured on the server");
        }

        String url = "https://finnhub.io/api/v1/news?category=market&token=" + apiKey;
        List<Map<String, Object>> newsList;
        try {
            String json = restTemplate.getForObject(url, String.class);
            newsList = objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            throw new AgentUnavailableException("Could not fetch market news from Finnhub", e);
        }

        if (newsList == null || newsList.isEmpty()) {
            throw new AgentUnavailableException("Finnhub returned no market news to analyze");
        }
        return normalize(newsList);
    }

    /** Pure normalization so tests can cover malformed payloads without HTTP. */
    static List<String> normalize(List<Map<String, Object>> newsList) {
        List<String> headlines = newsList.stream()
                .filter(a -> a != null && a.get("headline") != null
                        && !String.valueOf(a.get("headline")).isBlank())
                .map(a -> a.get("headline") + " (" + a.getOrDefault("source", "unknown") + ")")
                .limit(MAX_HEADLINES)
                .collect(Collectors.toList());
        if (headlines.isEmpty()) {
            throw new AgentUnavailableException("Finnhub news payload had no usable headlines");
        }
        return headlines;
    }
}
