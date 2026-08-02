package com.postfolio.postfolio.stockInvestmentAgents.capital;

import com.fasterxml.jackson.databind.JsonNode;
import com.postfolio.postfolio.stockInvestmentAgents.groq.GroqClient;
import com.postfolio.postfolio.stockInvestmentAgents.groq.GroqConfig;
import com.postfolio.postfolio.stockInvestmentAgents.model.Candidate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Shared LLM plumbing for the three competing allocators (docs/agent-trader-v2.md §6.3–6.5).
 * Subclasses differ only in style name and bias prompt.
 */
public abstract class AbstractAllocator {

    protected final GroqClient groq;
    protected final GroqConfig config;

    protected AbstractAllocator(GroqClient groq, GroqConfig config) {
        this.groq = groq;
        this.config = config;
    }

    public abstract String style();

    protected abstract String biasPrompt();

    public AllocationProposal propose(List<Candidate> winners, double deployable, Map<String, Double> quotes) {
        StringBuilder brief = new StringBuilder();
        for (Candidate c : winners) {
            brief.append("- ").append(c.ticker)
                    .append(" | judge score: ").append(c.score)
                    .append(" | price: $").append(quotes.get(c.ticker))
                    .append(" | thesis: ").append(c.thesis)
                    .append('\n');
        }
        String system = """
                You are the %s Allocator on a capital committee managing a small demo book.
                Deployable capital: $%.2f (a cash reserve has already been carved out).
                Bias: %s
                Respond with ONLY JSON: {"style":"%s","proposal":{"NVDA":400},"argument":"..."}
                Amounts are dollar notionals; the sum must be <= deployable capital; only use the listed tickers.
                """.formatted(style(), deployable, biasPrompt(), style());

        JsonNode json = groq.chatJson(config.getFastModel(), system, "Shortlist:\n" + brief);
        Map<String, Double> proposal = sanitize(json.path("proposal"), winners, deployable);
        return new AllocationProposal(style(), proposal, json.path("argument").asText(""));
    }

    /** Drops unknown tickers / bad amounts and scales down if the model overspent. */
    static Map<String, Double> sanitize(JsonNode raw, List<Candidate> winners, double deployable) {
        Map<String, Double> cleaned = new LinkedHashMap<>();
        double total = 0;
        for (Candidate c : winners) {
            double amount = raw.path(c.ticker).asDouble(0);
            if (amount > 0) {
                cleaned.put(c.ticker, amount);
                total += amount;
            }
        }
        if (total > deployable && total > 0) {
            double scale = deployable / total;
            cleaned.replaceAll((t, amount) -> Math.floor(amount * scale));
        }
        return cleaned;
    }
}
