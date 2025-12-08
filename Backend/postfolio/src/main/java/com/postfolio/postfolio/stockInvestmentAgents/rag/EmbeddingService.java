package com.postfolio.postfolio.stockInvestmentAgents.rag;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class EmbeddingService {

    private static final String OLLAMA_URL = "http://localhost:11434/api/embeddings";
    private static final String MODEL = "nomic-embed-text";

    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    public float[] embed(String text) {
        try {
            String requestBody = mapper.writeValueAsString(
                    new EmbeddingRequest(MODEL, text)
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(OLLAMA_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());

            JsonNode root = mapper.readTree(response.body());
            JsonNode embeddingNode = root.get("embedding");

            float[] embedding = new float[embeddingNode.size()];
            for (int i = 0; i < embeddingNode.size(); i++) {
                embedding[i] = embeddingNode.get(i).floatValue();
            }

            return embedding;

        } catch (Exception e) {
            throw new RuntimeException("Failed to create embedding", e);
        }
    }

    // inner request payload
    private static class EmbeddingRequest {
        public String model;
        public String prompt;

        public EmbeddingRequest(String model, String prompt) {
            this.model = model;
            this.prompt = prompt;
        }
    }
}