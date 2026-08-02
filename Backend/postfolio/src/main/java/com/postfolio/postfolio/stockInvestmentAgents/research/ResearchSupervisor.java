package com.postfolio.postfolio.stockInvestmentAgents.research;

import com.postfolio.postfolio.stockInvestmentAgents.AgentUnavailableException;
import com.postfolio.postfolio.stockInvestmentAgents.model.RunResult;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Owns the Finnhub-replacement research crew (docs/agent-trader-v3.md §5).
 * Writes evidence + traces onto the RunResult and returns headlines for debate.
 */
@Service
public class ResearchSupervisor {

    private final SourcePlanner planner;
    private final WebScout webScout;
    private final EvidencePacker packer;
    private final PriceScout priceScout;

    public ResearchSupervisor(SourcePlanner planner, WebScout webScout,
                              EvidencePacker packer, PriceScout priceScout) {
        this.planner = planner;
        this.webScout = webScout;
        this.packer = packer;
        this.priceScout = priceScout;
    }

    /** Plan → scout → pack. Returns headline strings for Bull/Bear. */
    public List<String> gatherEvidence(RunResult result) {
        List<String> targets = planner.plan(8);
        result.addTrace("source_planner", "ok",
                "Planned %d public source(s)".formatted(targets.size()),
                Map.of("targets", targets, "provider", "web"));

        WebScout.ScoutReport report = webScout.scout(targets);
        result.addTrace("web_scout", report.fetchedOk() > 0 ? "ok" : "error",
                "Fetched %d source(s), skipped %d".formatted(report.fetchedOk(), report.skipped().size()),
                Map.of("skipped", report.skipped(), "rawItems", report.items().size()));

        List<EvidenceItem> packed = packer.pack(report.items());
        if (packed.isEmpty()) {
            throw new AgentUnavailableException(
                    "Web research returned no usable headlines — check network or allowlist sources");
        }
        result.evidencePack = packer.toDto(packed);
        result.addTrace("evidence_packer", "ok",
                "Packed %d evidence item(s) for debate".formatted(packed.size()),
                Map.of("count", packed.size(), "sampleUrls",
                        packed.stream().limit(5).map(i -> i.sourceUrl).toList()));
        return packer.headlines(packed);
    }

    /** One quote snapshot for advanced tickers. */
    public Map<String, Double> quoteSnapshot(RunResult result, List<String> tickers) {
        Map<String, Double> quotes = priceScout.snapshot(tickers);
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("quotes", quotes);
        detail.put("provider", "yahoo_chart");
        result.addTrace("price_scout", quotes.isEmpty() ? "error" : "ok",
                "Quoted %d of %d ticker(s)".formatted(quotes.size(), tickers.size()),
                detail);
        return quotes;
    }
}
