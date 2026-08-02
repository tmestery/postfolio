package com.postfolio.postfolio.stockInvestmentAgents.debate;

import com.fasterxml.jackson.databind.JsonNode;
import com.postfolio.postfolio.stockInvestmentAgents.groq.GroqClient;
import com.postfolio.postfolio.stockInvestmentAgents.groq.GroqConfig;
import com.postfolio.postfolio.stockInvestmentAgents.model.Candidate;
import org.springframework.stereotype.Service;

import java.util.List;

/** Attacks/qualifies each bull candidate (docs/agent-trader-v2.md §5.4). */
@Service
public class BearAgent {

    private final GroqClient groq;
    private final GroqConfig config;

    public BearAgent(GroqClient groq, GroqConfig config) {
        this.groq = groq;
        this.config = config;
    }

    /** Attaches risks + severityDown onto the candidates in place. */
    public void critique(List<String> headlines, List<Candidate> candidates) {
        if (candidates.isEmpty()) return;

        StringBuilder picks = new StringBuilder();
        for (Candidate c : candidates) {
            picks.append("- ").append(c.ticker).append(": ").append(c.thesis).append('\n');
        }
        String system = """
                You are the Bear Agent on an investment research desk. Attack or qualify each bull thesis.
                Respond with ONLY JSON:
                {"critiques":[{"ticker":"NVDA","risks":["..."],"severityDown":0.15,"reject":false}]}
                severityDown is 0-1 (how much the thesis should be discounted). Set reject true only for
                clearly uninvestable ideas. No prose outside JSON.
                """;
        String user = "Bull candidates:\n" + picks + "\nRecent headlines:\n" + String.join("\n", headlines);

        JsonNode json = groq.chatJson(config.getFastModel(), system, user);
        for (JsonNode node : json.path("critiques")) {
            String ticker = node.path("ticker").asText("").trim().toUpperCase();
            candidates.stream().filter(c -> c.ticker.equals(ticker)).findFirst().ifPresent(c -> {
                for (JsonNode risk : node.path("risks")) {
                    c.risks.add(risk.asText());
                }
                c.severityDown = Math.max(0, Math.min(1, node.path("severityDown").asDouble(0)));
                if (node.path("reject").asBoolean(false)) {
                    c.decision = "reject";
                    c.rationale = "Bear agent flagged as uninvestable";
                }
            });
        }
    }
}
