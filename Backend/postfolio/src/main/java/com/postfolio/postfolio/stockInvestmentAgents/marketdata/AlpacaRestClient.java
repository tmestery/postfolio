package com.postfolio.postfolio.stockInvestmentAgents.marketdata;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Alpaca Market Data REST — IEX snapshots on Basic plan (no full SIP).
 * One batched call per refresh keeps us under 200 calls/min.
 */
@Component
public class AlpacaRestClient {

    private final AlpacaConfig config;
    private final HttpClient http;
    private final ObjectMapper mapper = new ObjectMapper();

    public AlpacaRestClient(AlpacaConfig config) {
        this.config = config;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(8))
                .build();
    }

    /** Latest marks for tickers; empty map if keys missing or HTTP fails. */
    public Map<String, Double> fetchLatestMarks(List<String> tickers) {
        if (!config.isConfigured() || tickers == null || tickers.isEmpty()) {
            return Map.of();
        }
        String symbols = tickers.stream()
                .filter(t -> t != null && !t.isBlank())
                .map(String::trim)
                .map(String::toUpperCase)
                .distinct()
                .collect(Collectors.joining(","));
        if (symbols.isBlank()) return Map.of();

        String url = config.getDataBaseUrl()
                + "/v2/stocks/snapshots?symbols="
                + symbols
                + "&feed="
                + config.getDataFeed();
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("APCA-API-KEY-ID", config.getApiKeyId())
                    .header("APCA-API-SECRET-KEY", config.getApiSecretKey())
                    .GET()
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) return Map.of();
            return parseSnapshots(response.body());
        } catch (Exception e) {
            return Map.of();
        }
    }

    /** Package-visible for tests. */
    Map<String, Double> parseSnapshots(String json) {
        try {
            JsonNode root = mapper.readTree(json);
            Map<String, Double> marks = new LinkedHashMap<>();
            root.fields().forEachRemaining(entry -> {
                String ticker = entry.getKey();
                Double price = extractMarkPrice(entry.getValue());
                if (price != null && price > 0) {
                    marks.put(ticker.toUpperCase(), price);
                }
            });
            return marks;
        } catch (Exception e) {
            return Map.of();
        }
    }

    private static Double extractMarkPrice(JsonNode snapshot) {
        if (snapshot == null || snapshot.isMissingNode()) return null;
        JsonNode quote = snapshot.path("latestQuote");
        double bid = quote.path("bp").asDouble(0);
        double ask = quote.path("ap").asDouble(0);
        if (bid > 0 && ask > 0) {
            return (bid + ask) / 2.0;
        }
        JsonNode trade = snapshot.path("latestTrade");
        double tradePrice = trade.path("p").asDouble(0);
        if (tradePrice > 0) return tradePrice;
        JsonNode daily = snapshot.path("dailyBar");
        double close = daily.path("c").asDouble(0);
        return close > 0 ? close : null;
    }
}
