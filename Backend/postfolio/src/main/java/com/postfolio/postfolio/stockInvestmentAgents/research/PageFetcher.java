package com.postfolio.postfolio.stockInvestmentAgents.research;

import java.util.Optional;

/** Fetch seam so tests can stub HTTP (docs/agent-trader-v3.md). */
public interface PageFetcher {
    Optional<String> get(String url);
}
