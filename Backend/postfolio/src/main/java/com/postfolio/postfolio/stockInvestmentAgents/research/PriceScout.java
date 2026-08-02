package com.postfolio.postfolio.stockInvestmentAgents.research;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Public quote snapshot without Finnhub (docs/agent-trader-v3.md §5.4).
 * Primary: Yahoo Finance chart JSON. Soft-fails per ticker.
 */
@Service
public class PriceScout {

    private final PageFetcher fetcher;
    private final ObjectMapper mapper = new ObjectMapper();

    public PriceScout(PageFetcher fetcher) {
        this.fetcher = fetcher;
    }

    public Map<String, Double> snapshot(List<String> tickers) {
        Map<String, Double> out = new LinkedHashMap<>();
        if (tickers == null) return out;
        for (String raw : tickers) {
            if (raw == null || raw.isBlank()) continue;
            String ticker = raw.trim().toUpperCase(Locale.ROOT);
            double price = fetchYahoo(ticker);
            if (price <= 0) price = fetchYahooFallback(ticker);
            if (price > 0 && price < 1_000_000) {
                out.put(ticker, round2(price));
            }
        }
        return out;
    }

    double fetchYahoo(String ticker) {
        String url = "https://query1.finance.yahoo.com/v8/finance/chart/"
                + ticker + "?interval=1d&range=1d";
        Optional<String> body = fetcher.get(url);
        if (body.isEmpty()) return 0;
        try {
            JsonNode meta = mapper.readTree(body.get())
                    .path("chart").path("result").path(0).path("meta");
            JsonNode price = meta.path("regularMarketPrice");
            if (price.isNumber()) return price.asDouble();
            JsonNode prev = meta.path("previousClose");
            if (prev.isNumber()) return prev.asDouble();
        } catch (Exception ignored) {
            // soft fail
        }
        return 0;
    }

    /** Secondary parse path — same host, different field layout resilience. */
    double fetchYahooFallback(String ticker) {
        return fetchYahoo(ticker); // single provider for v3.0; hook for #2 later
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
