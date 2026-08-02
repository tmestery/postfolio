package com.postfolio.postfolio.stockInvestmentAgents.groq;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.postfolio.postfolio.stockInvestmentAgents.AgentUnavailableException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GroqClientTests {

    private final ObjectMapper mapper = new ObjectMapper();

    private static GroqConfig config(String key) {
        return new GroqConfig(key, "fast-model", "judge-model", 1000, 0.15, 60000);
    }

    // Positive: clean JSON content parses.
    @Test
    void extractJsonParsesPlainJson() {
        JsonNode node = GroqClient.extractJson(mapper, "{\"candidates\":[{\"ticker\":\"NVDA\"}]}");
        assertEquals("NVDA", node.path("candidates").path(0).path("ticker").asText());
    }

    // Negative: markdown-fenced output is stripped before parsing.
    @Test
    void extractJsonStripsMarkdownFences() {
        JsonNode node = GroqClient.extractJson(mapper, "```json\n{\"ok\":true}\n```");
        assertTrue(node.path("ok").asBoolean());
    }

    // Negative: invalid JSON maps to AgentUnavailableException.
    @Test
    void extractJsonRejectsInvalidJson() {
        assertThrows(AgentUnavailableException.class,
                () -> GroqClient.extractJson(mapper, "I think NVDA is a buy because..."));
    }

    // Negative: empty output maps to AgentUnavailableException.
    @Test
    void extractJsonRejectsEmptyOutput() {
        assertThrows(AgentUnavailableException.class, () -> GroqClient.extractJson(mapper, "  "));
    }

    // Negative: missing API key fails fast without any HTTP call.
    @Test
    void chatWithoutApiKeyThrowsClearError() {
        GroqClient client = new GroqClient(config(""));
        AgentUnavailableException e = assertThrows(AgentUnavailableException.class,
                () -> client.chat("fast-model", "sys", "user"));
        assertTrue(e.getMessage().contains("GROQ_API_KEY"));
    }
}
