package com.postfolio.postfolio.stockInvestmentAgents.debate;

import com.fasterxml.jackson.databind.JsonNode;
import com.postfolio.postfolio.stockInvestmentAgents.groq.GroqClient;
import com.postfolio.postfolio.stockInvestmentAgents.groq.GroqConfig;
import com.postfolio.postfolio.stockInvestmentAgents.model.Candidate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Scores the bull/bear debate with a fixed rubric (docs/agent-trader-v2.md §5.5). */
@Service
public class StockJudgeAgent {

    static final int MAX_ADVANCED = 5;

    private final GroqClient groq;
    private final GroqConfig config;

    public StockJudgeAgent(GroqClient groq, GroqConfig config) {
        this.groq = groq;
        this.config = config;
    }

    public record Verdict(List<Candidate> advanced, List<Map<String, String>> rejected) {}

    public Verdict judge(List<Candidate> candidates) {
        List<Candidate> debatable = new ArrayList<>();
        List<Map<String, String>> rejected = new ArrayList<>();
        for (Candidate c : candidates) {
            if ("reject".equals(c.decision)) {
                rejected.add(Map.of("ticker", c.ticker, "reason", c.rationale));
            } else {
                debatable.add(c);
            }
        }
        if (debatable.isEmpty()) return new Verdict(List.of(), rejected);

        StringBuilder brief = new StringBuilder();
        for (Candidate c : debatable) {
            brief.append("- ").append(c.ticker)
                    .append(" | thesis: ").append(c.thesis)
                    .append(" | bull confidence: ").append(c.confidence)
                    .append(" | bear risks: ").append(String.join("; ", c.risks))
                    .append(" | bear severityDown: ").append(c.severityDown)
                    .append('\n');
        }
        String system = """
                You are the Stock Judge. Score each candidate 0-10 using this rubric:
                (1) news support strength, (2) thesis clarity, (3) risk acknowledgment, (4) investability.
                Advance at most %d. Respond with ONLY JSON:
                {"ranked":[{"ticker":"NVDA","score":8.1,"decision":"advance","rationale":"..."}],
                 "rejected":[{"ticker":"XYZ","reason":"..."}]}
                decision is "advance" or "reject". No prose outside JSON.
                """.formatted(MAX_ADVANCED);

        JsonNode json = groq.chatJson(config.getJudgeModel(), system, brief.toString());

        List<Candidate> advanced = new ArrayList<>();
        for (JsonNode node : json.path("ranked")) {
            String ticker = node.path("ticker").asText("").trim().toUpperCase();
            debatable.stream().filter(c -> c.ticker.equals(ticker)).findFirst().ifPresent(c -> {
                c.score = node.path("score").asDouble(0);
                c.decision = node.path("decision").asText("reject");
                c.rationale = node.path("rationale").asText("");
                if ("advance".equals(c.decision) && advanced.size() < MAX_ADVANCED) {
                    advanced.add(c);
                } else if (!"advance".equals(c.decision)) {
                    rejected.add(Map.of("ticker", c.ticker,
                            "reason", c.rationale.isBlank() ? "Judge rejected" : c.rationale));
                }
            });
        }
        for (JsonNode node : json.path("rejected")) {
            String ticker = node.path("ticker").asText("").trim().toUpperCase();
            if (advanced.stream().noneMatch(c -> c.ticker.equals(ticker))
                    && rejected.stream().noneMatch(r -> ticker.equals(r.get("ticker")))
                    && debatable.stream().anyMatch(c -> c.ticker.equals(ticker))) {
                rejected.add(Map.of("ticker", ticker, "reason", node.path("reason").asText("Judge rejected")));
            }
        }
        advanced.sort((a, b) -> Double.compare(b.score, a.score));
        return new Verdict(advanced, rejected);
    }
}
