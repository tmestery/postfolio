package com.postfolio.postfolio.stockInvestmentAgents.debate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Shared helper for feeding canned JSON to mocked GroqClients. */
public final class GroqClientTestSupport {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private GroqClientTestSupport() {}

    public static JsonNode parse(String json) {
        try {
            return MAPPER.readTree(json);
        } catch (Exception e) {
            throw new IllegalArgumentException("Bad test JSON", e);
        }
    }
}
