package com.postfolio.postfolio.stockInvestmentAgents.debate;

import com.fasterxml.jackson.databind.JsonNode;
import com.postfolio.postfolio.stockInvestmentAgents.groq.GroqClient;
import com.postfolio.postfolio.stockInvestmentAgents.groq.GroqConfig;
import com.postfolio.postfolio.stockInvestmentAgents.model.Candidate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Argues FOR investments from the headline pack (docs/agent-trader-v2.md §5.3). */
@Service
public class BullAgent {

    static final int MAX_CANDIDATES = 8;
    static final Pattern TICKER = Pattern.compile("^[A-Z]{1,5}$");

    private final GroqClient groq;
    private final GroqConfig config;

    public BullAgent(GroqClient groq, GroqConfig config) {
        this.groq = groq;
        this.config = config;
    }

    public List<Candidate> propose(List<String> headlines) {
        String system = """
                You are the Bull Agent on an investment research desk.
                From market headlines, propose up to %d US-listed stock candidates worth buying.
                Respond with ONLY JSON: {"candidates":[{"ticker":"NVDA","thesis":"...","confidence":0.82}]}
                Rules: tickers are 1-5 uppercase letters; confidence is 0-1; no ETFs, no crypto; no prose outside JSON.
                """.formatted(MAX_CANDIDATES);
        String user = "Market headlines:\n" + String.join("\n", headlines);

        JsonNode json = groq.chatJson(config.getFastModel(), system, user);
        List<Candidate> candidates = new ArrayList<>();
        for (JsonNode node : json.path("candidates")) {
            String ticker = node.path("ticker").asText("").trim().toUpperCase();
            if (!TICKER.matcher(ticker).matches()) continue;
            if (candidates.stream().anyMatch(c -> c.ticker.equals(ticker))) continue;
            Candidate c = new Candidate(ticker,
                    node.path("thesis").asText("No thesis provided"),
                    clamp(node.path("confidence").asDouble(0.5)));
            candidates.add(c);
            if (candidates.size() >= MAX_CANDIDATES) break;
        }
        return candidates;
    }

    private static double clamp(double v) {
        return Math.max(0, Math.min(1, v));
    }
}
