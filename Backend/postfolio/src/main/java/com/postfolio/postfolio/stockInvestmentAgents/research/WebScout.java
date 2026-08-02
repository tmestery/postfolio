package com.postfolio.postfolio.stockInvestmentAgents.research;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/** Parallel fetch+extract over allowlisted URLs (docs/agent-trader-v3.md §5.2–5.3). */
@Service
public class WebScout {

    public static final int MAX_PARALLEL = 3;
    public static final int MAX_SUCCESSFUL = 8;

    private final PageFetcher fetcher;
    private final HtmlTextExtractor extractor;
    private final SourceAllowlist allowlist;

    public WebScout(PageFetcher fetcher, HtmlTextExtractor extractor, SourceAllowlist allowlist) {
        this.fetcher = fetcher;
        this.extractor = extractor;
        this.allowlist = allowlist;
    }

    public record ScoutReport(List<EvidenceItem> items, List<String> skipped, int fetchedOk) {}

    public ScoutReport scout(List<String> urls) {
        List<EvidenceItem> items = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        if (urls == null || urls.isEmpty()) return new ScoutReport(items, skipped, 0);

        Semaphore permits = new Semaphore(MAX_PARALLEL);
        ExecutorService pool = Executors.newFixedThreadPool(MAX_PARALLEL);
        try {
            List<Future<List<EvidenceItem>>> futures = new ArrayList<>();
            for (String url : urls) {
                if (!allowlist.isAllowed(url)) {
                    skipped.add(url + " (not allowlisted)");
                    continue;
                }
                futures.add(pool.submit(fetchOne(url, permits, skipped)));
            }
            int ok = 0;
            for (Future<List<EvidenceItem>> future : futures) {
                if (ok >= MAX_SUCCESSFUL) break;
                try {
                    List<EvidenceItem> got = future.get(15, TimeUnit.SECONDS);
                    if (got != null && !got.isEmpty()) {
                        items.addAll(got);
                        ok++;
                    }
                } catch (Exception e) {
                    skipped.add("fetch timeout/error");
                }
            }
            return new ScoutReport(items, skipped, ok);
        } finally {
            pool.shutdownNow();
        }
    }

    private Callable<List<EvidenceItem>> fetchOne(String url, Semaphore permits, List<String> skipped) {
        return () -> {
            permits.acquire();
            try {
                Optional<String> body = fetcher.get(url);
                if (body.isEmpty()) {
                    synchronized (skipped) {
                        skipped.add(url + " (empty/blocked)");
                    }
                    return List.of();
                }
                return extractor.extract(url, body.get());
            } finally {
                permits.release();
            }
        };
    }
}
