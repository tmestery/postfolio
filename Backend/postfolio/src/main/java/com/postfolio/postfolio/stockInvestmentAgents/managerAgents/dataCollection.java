package com.postfolio.postfolio.stockInvestmentAgents.managerAgents;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.postfolio.postfolio.stockInvestmentAgents.AgentUnavailableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class dataCollection {

    private static final int MAX_HEADLINES = 75;
    private static final int CHUNK_SIZE = 25;

    private final String apiKey;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public dataCollection(@Value("${FINNHUB_API_KEY:}") String apiKey) {
        this.apiKey = apiKey;
    }

    // Gets the general market news + condenses it into a list of short summaries
    public List<List<String>> fetchGeneralMarketNews() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new AgentUnavailableException(
                    "FINNHUB_API_KEY is not configured on the server");
        }

        String url = "https://finnhub.io/api/v1/news?category=market&token=" + apiKey;

        List<Map<String, Object>> newsList;
        try {
            String jsonResponse = restTemplate.getForObject(url, String.class);
            newsList = objectMapper.readValue(
                    jsonResponse,
                    new TypeReference<List<Map<String, Object>>>() {}
            );
        } catch (Exception e) {
            throw new AgentUnavailableException(
                    "Could not fetch market news from Finnhub", e);
        }

        if (newsList == null || newsList.isEmpty()) {
            throw new AgentUnavailableException(
                    "Finnhub returned no market news to analyze");
        }

        List<String> fullList = newsList.stream()
                .map(article -> article.get("headline") + " (" + article.get("source") + ")")
                .limit(MAX_HEADLINES)
                .collect(Collectors.toList());

        return chunk(fullList, CHUNK_SIZE);
    }

    /** Splits into sublists of up to {@code size} without assuming a fixed total. */
    static List<List<String>> chunk(List<String> items, int size) {
        List<List<String>> grouped = new ArrayList<>();
        for (int start = 0; start < items.size(); start += size) {
            grouped.add(new ArrayList<>(items.subList(start, Math.min(start + size, items.size()))));
        }
        return grouped;
    }
}
