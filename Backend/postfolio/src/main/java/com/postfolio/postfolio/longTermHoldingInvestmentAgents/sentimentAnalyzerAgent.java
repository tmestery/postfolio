package com.postfolio.postfolio.longTermHoldingInvestmentAgents;

import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;

// Takes information from analyzer and dataCollector and makes final decision utilizing Ollama
public class sentimentAnalyzerAgent {
    private final String OLLAMA_URL = "http://localhost:11434/api/generate";
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void sentimentAnalyzer() {
        // Building the prompt:
        String prompt = "";

        Map<String, Object> requestBody = Map.of(
                "model", "llama3",
                "stream", false,
                "prompt", prompt
        );

        String responseString = restTemplate.postForObject(OLLAMA_URL, requestBody, String.class);

        try {
            // Parse Ollama JSON that's received
            Map<String, Object> aiResult = objectMapper.readValue(responseString, Map.class);

            // Ollama typically returns something like: {"results":[{"content":"..."}]}
            List<Map<String, Object>> results = (List<Map<String, Object>>) aiResult.get("results");
            if (results != null && !results.isEmpty()) {
                String tip = (String) results.get(0).get("content");
                if (tip != null && !tip.isEmpty()) {
                    return;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}