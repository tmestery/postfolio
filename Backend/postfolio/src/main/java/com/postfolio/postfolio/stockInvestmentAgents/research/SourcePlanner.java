package com.postfolio.postfolio.stockInvestmentAgents.research;

import com.fasterxml.jackson.databind.JsonNode;
import com.postfolio.postfolio.stockInvestmentAgents.groq.GroqClient;
import com.postfolio.postfolio.stockInvestmentAgents.groq.GroqConfig;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Picks allowlisted public URLs to scrape (docs/agent-trader-v3.md §5.1).
 * Falls back to the default seed list when Groq is unavailable or returns junk.
 */
@Service
public class SourcePlanner {

    private final GroqClient groq;
    private final GroqConfig config;
    private final SourceAllowlist allowlist;

    public SourcePlanner(GroqClient groq, GroqConfig config, SourceAllowlist allowlist) {
        this.groq = groq;
        this.config = config;
        this.allowlist = allowlist;
    }

    public List<String> plan(int maxTargets) {
        int cap = Math.max(1, Math.min(maxTargets, 12));
        List<String> defaults = allowlist.defaultTargets();
        if (config.getApiKey() == null || config.getApiKey().isBlank()) {
            return defaults.stream().limit(cap).toList();
        }
        try {
            String system = """
                    You plan public web research for a demo stock desk.
                    Return JSON: {"targets":[{"url":"...","why":"..."}]}.
                    Only choose from the provided allowlisted URLs. Prefer RSS feeds.
                    Cap at %d targets. No commentary outside JSON.
                    """.formatted(cap);
            String user = "Allowlisted URLs:\n" + String.join("\n", defaults);
            JsonNode root = groq.chatJson(config.getFastModel(), system, user);
            LinkedHashSet<String> urls = new LinkedHashSet<>();
            if (root.path("targets").isArray()) {
                for (JsonNode t : root.path("targets")) {
                    String url = t.path("url").asText("");
                    if (allowlist.isAllowed(url)) urls.add(url.trim());
                }
            }
            if (urls.isEmpty()) return defaults.stream().limit(cap).toList();
            return new ArrayList<>(urls).stream().limit(cap).toList();
        } catch (Exception e) {
            return defaults.stream().limit(cap).toList();
        }
    }
}
