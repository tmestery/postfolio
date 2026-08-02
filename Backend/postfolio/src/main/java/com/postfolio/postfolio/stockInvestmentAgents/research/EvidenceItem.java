package com.postfolio.postfolio.stockInvestmentAgents.research;

import java.util.ArrayList;
import java.util.List;

/** One scraped evidence row (docs/agent-trader-v3.md §5.2). */
public class EvidenceItem {
    public String title = "";
    public String sourceUrl = "";
    public List<String> bullets = new ArrayList<>();
    public List<String> tickers = new ArrayList<>();

    public EvidenceItem() {}

    public EvidenceItem(String title, String sourceUrl) {
        this.title = title == null ? "" : title;
        this.sourceUrl = sourceUrl == null ? "" : sourceUrl;
    }

    /** Compact headline string for Bull/Bear prompts. */
    public String asHeadline() {
        String host = sourceUrl;
        try {
            host = java.net.URI.create(sourceUrl).getHost();
        } catch (Exception ignored) {
            // keep raw url fragment
        }
        if (host == null || host.isBlank()) host = "web";
        return title + " (" + host + ")";
    }
}
