package com.postfolio.postfolio.stockInvestmentAgents.research;

import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

/** Bounded public HTTP GET for research scouts. */
@Component
public class HttpPageFetcher implements PageFetcher {

    static final int MAX_BYTES = 250_000;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    @Override
    public Optional<String> get(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(12))
                    .header("User-Agent", "PostfolioResearchBot/1.0 (+https://github.com/tmestery/postfolio; demo)")
                    .header("Accept", "application/rss+xml, application/xml, text/xml, text/html, application/json, */*")
                    .GET()
                    .build();
            HttpResponse<byte[]> response = http.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() >= 400) return Optional.empty();
            byte[] body = response.body();
            if (body == null || body.length == 0) return Optional.empty();
            if (body.length > MAX_BYTES) {
                body = java.util.Arrays.copyOf(body, MAX_BYTES);
            }
            return Optional.of(new String(body, StandardCharsets.UTF_8));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
