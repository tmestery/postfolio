package com.postfolio.postfolio.longTermHoldingInvestmentAgents;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.List;
import java.util.Map;

@Service
public class sentimentAnalyzerAgent {
    private final String OLLAMA_URL = "http://localhost:11434/api/generate";
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String sentimentAnalyzer(List<String> marketNewsList) {
        try {
            // Convert news list to JSON string
            String newsJson = objectMapper.writeValueAsString(marketNewsList);

            // Build the prompt dynamically
            // Build the prompt dynamically for predicting top stock
            String prompt = "You are a financial data assistant. I will give you a list of news headlines.\n\n" +
                    "Your task:\n" +
                    "- Analyze the headlines and identify the ONE stock you firmly believe will perform the best.\n" +
                    "- Consider sentiment, sector trends, and market impact implied by the headlines.\n" +
                    "- Output ONLY the single stock TICKER SYMBOL you are most confident in.\n" +
                    "- Do NOT output explanations, extra text, punctuation, or multiple stocks.\n" +
                    "- If no stock is mentioned, respond with 'None'.\n\n" +
                    "Use the data from the following headlines to make your prediction:\n" + newsJson;

            // Build request body for Ollama
            Map<String, Object> requestBody = Map.of(
                    "model", "llama3",
                    "stream", false,
                    "prompt", prompt
            );

            // Send POST request
            String responseString = restTemplate.postForObject(OLLAMA_URL, requestBody, String.class);

            // Parse Ollama response
            Map<String, Object> aiResult = objectMapper.readValue(responseString, Map.class);
            String stock = aiResult.get("response").toString();
            return stock;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "stock here";
    }
}