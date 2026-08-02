package com.postfolio.postfolio.stockInvestmentAgents.groq;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.postfolio.postfolio.stockInvestmentAgents.AgentUnavailableException;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Minimal OpenAI-compatible chat client for Groq.
 * All LLM sub-agents go through here so tests can mock a single seam.
 */
@Component
public class GroqClient {

    private static final String CHAT_URL = "https://api.groq.com/openai/v1/chat/completions";

    private final GroqConfig config;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final ObjectMapper mapper = new ObjectMapper();

    public GroqClient(GroqConfig config) {
        this.config = config;
    }

    /** Sends one chat completion and returns the raw assistant message content. */
    public String chat(String model, String systemPrompt, String userPrompt) {
        if (config.getApiKey() == null || config.getApiKey().isBlank()) {
            throw new AgentUnavailableException("GROQ_API_KEY is not configured on the server");
        }
        try {
            String body = mapper.writeValueAsString(Map.of(
                    "model", model,
                    "temperature", 0.4,
                    "response_format", Map.of("type", "json_object"),
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", userPrompt))));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(CHAT_URL))
                    .timeout(Duration.ofSeconds(45))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + config.getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 401 || response.statusCode() == 403) {
                throw new AgentUnavailableException("Groq rejected the API key (" + response.statusCode() + ")");
            }
            if (response.statusCode() == 429) {
                throw new AgentUnavailableException("Groq rate limit hit — try again shortly");
            }
            if (response.statusCode() >= 400) {
                throw new AgentUnavailableException("Groq request failed (" + response.statusCode() + ")");
            }

            JsonNode content = mapper.readTree(response.body())
                    .path("choices").path(0).path("message").path("content");
            if (content.isMissingNode() || content.asText().isBlank()) {
                throw new AgentUnavailableException("Groq returned an empty completion");
            }
            return content.asText();
        } catch (AgentUnavailableException e) {
            throw e;
        } catch (Exception e) {
            throw new AgentUnavailableException("Groq is not reachable (" + model + ")", e);
        }
    }

    /** Chat, then parse the content as JSON (handles stray markdown fences). */
    public JsonNode chatJson(String model, String systemPrompt, String userPrompt) {
        return extractJson(mapper, chat(model, systemPrompt, userPrompt));
    }

    /** Parses model output into JSON, stripping ```json fences if present. */
    static JsonNode extractJson(ObjectMapper mapper, String content) {
        if (content == null || content.isBlank()) {
            throw new AgentUnavailableException("Model returned empty output");
        }
        String cleaned = content.trim();
        if (cleaned.startsWith("```")) {
            int firstNewline = cleaned.indexOf('\n');
            int lastFence = cleaned.lastIndexOf("```");
            if (firstNewline >= 0 && lastFence > firstNewline) {
                cleaned = cleaned.substring(firstNewline + 1, lastFence).trim();
            }
        }
        try {
            return mapper.readTree(cleaned);
        } catch (Exception e) {
            throw new AgentUnavailableException("Model returned invalid JSON", e);
        }
    }
}
