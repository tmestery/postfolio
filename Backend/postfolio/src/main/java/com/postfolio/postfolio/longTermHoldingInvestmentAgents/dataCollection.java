package com.postfolio.postfolio.longTermHoldingInvestmentAgents;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.stream.IntStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class dataCollection {

    private final String apiKey = System.getenv("FINNHUB_API_KEY");
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Gets the general market news + condenses it into a list of short summaries
    public List<List<String>> fetchGeneralMarketNews() {
       String url = "https://finnhub.io/api/v1/news?category=market&token=" + apiKey;

        try {
            String jsonResponse = restTemplate.getForObject(url, String.class);

            List<Map<String, Object>> newsList = objectMapper.readValue(
                    jsonResponse,
                    new TypeReference<List<Map<String, Object>>>() {}
            );

            List<String> fullList = newsList.stream()
                    .map(article -> article.get("headline") + " (" + article.get("source") + ")")
                    .limit(75)
                    .collect(Collectors.toList());

            // Split into 3 sublists of 25
            List<List<String>> grouped = IntStream.range(0, 3)
                    .mapToObj(i -> fullList.subList(i * 25, (i + 1) * 25))
                    .collect(Collectors.toList());

            return grouped;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}