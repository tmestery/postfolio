package com.postfolio.postfolio.longTermHoldingInvestmentAgents;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class dataCollectorAgent {

    private final String apiKey = System.getenv("FINNHUB_API_KEY");
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Gets the general market news + condenses it into a list of short summaries
    public List<String> fetchGeneralMarketNews() {
       String url = "https://finnhub.io/api/v1/news?category=market&token=" + apiKey;

        try {
            String jsonResponse = restTemplate.getForObject(url, String.class);

            List<Map<String, Object>> newsList = objectMapper.readValue(
                    jsonResponse,
                    new TypeReference<List<Map<String, Object>>>() {}
            );

            // Condense each article to a short string
            return newsList.stream()
                    .map(article -> article.get("headline") + " (" + article.get("source") + ")")
                    .limit(15)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}