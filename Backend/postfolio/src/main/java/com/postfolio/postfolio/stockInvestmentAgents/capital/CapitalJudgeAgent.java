package com.postfolio.postfolio.stockInvestmentAgents.capital;

import com.fasterxml.jackson.databind.JsonNode;
import com.postfolio.postfolio.stockInvestmentAgents.groq.GroqClient;
import com.postfolio.postfolio.stockInvestmentAgents.groq.GroqConfig;
import com.postfolio.postfolio.stockInvestmentAgents.model.Candidate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Merges the competing allocator proposals into one approved book
 * (docs/agent-trader-v2.md §6.7). Output is re-validated in code:
 * unknown tickers dropped, overspend scaled back, cashHeld recomputed.
 */
@Service
public class CapitalJudgeAgent {

    private final GroqClient groq;
    private final GroqConfig config;

    public CapitalJudgeAgent(GroqClient groq, GroqConfig config) {
        this.groq = groq;
        this.config = config;
    }

    public Map<String, Object> decide(List<Candidate> winners,
                                      List<AllocationProposal> proposals,
                                      Map<String, Object> cashGuardReport,
                                      double startingAllowance, double reserveTarget) {
        double deployable = startingAllowance - reserveTarget;

        StringBuilder brief = new StringBuilder("Shortlist (judge scores):\n");
        for (Candidate c : winners) {
            brief.append("- ").append(c.ticker).append(": ").append(c.score).append('\n');
        }
        brief.append("\nProposals:\n");
        for (AllocationProposal p : proposals) {
            brief.append("- ").append(p.style).append(": ").append(p.proposal)
                    .append(" | argument: ").append(p.argument).append('\n');
        }
        brief.append("\nCash Guard report: ").append(cashGuardReport).append('\n');

        String system = """
                You are the Capital Judge. Score each allocation proposal 0-10 on:
                (1) alignment with judge rankings, (2) diversification fitness,
                (3) cash discipline per the Cash Guard, (4) feasibility.
                Then emit ONE approved book (you may blend proposals). Total approved <= $%.2f.
                Respond with ONLY JSON:
                {"approved":{"NVDA":350},"cashHeld":200,"winnerStyle":"blend",
                 "scores":{"aggressive":6.2,"balanced":8.4,"defensive":7.1},"rationale":"..."}
                """.formatted(deployable);

        JsonNode json = groq.chatJson(config.getJudgeModel(), system, brief.toString());

        Map<String, Double> approved = new LinkedHashMap<>();
        double total = 0;
        for (Candidate c : winners) {
            double amount = json.path("approved").path(c.ticker).asDouble(0);
            if (amount > 0) {
                approved.put(c.ticker, amount);
                total += amount;
            }
        }
        if (total > deployable && total > 0) {
            double scale = deployable / total;
            approved.replaceAll((t, amount) -> Math.floor(amount * scale));
            total = approved.values().stream().mapToDouble(Double::doubleValue).sum();
        }

        Map<String, Object> decision = new LinkedHashMap<>();
        decision.put("approved", approved);
        decision.put("cashHeld", startingAllowance - total);
        decision.put("winnerStyle", json.path("winnerStyle").asText("blend"));
        Map<String, Double> scores = new LinkedHashMap<>();
        for (AllocationProposal p : proposals) {
            scores.put(p.style, json.path("scores").path(p.style).asDouble(0));
        }
        decision.put("scores", scores);
        decision.put("rationale", json.path("rationale").asText(""));
        return decision;
    }
}
